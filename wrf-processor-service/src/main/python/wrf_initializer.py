"""
WRF 初始化器 — 将数据同化分析场反哺 WRF 模型的闭环桥梁

数据流:
  无人机/探空/地面站实况观测
    → uav-weather-collector 汇聚
    → data-assimilation-service 贝叶斯同化
    → 分析场 (analysis field)
    → wrf_initializer 生成 WRF 兼容的 NetCDF
    → WRF 模式重新预报
    → 新预报场用于路径规划

支持两种反馈模式:
  - REPLACE: 以原始 wrfinput 为模板，替换被同化的变量
  - NUDGING: 生成 grid nudging 文件 (wrffdda)，WRF 运行时持续松弛
"""

import os
import sys
import json
import logging
from typing import Dict, List, Optional
from dataclasses import dataclass, field

import numpy as np

try:
    from netCDF4 import Dataset
except ImportError:
    Dataset = None

logger = logging.getLogger(__name__)


# === WRF 变量名映射 ===
# 从系统内部变量名 -> WRF 原生 NetCDF 变量名
VARNAME_TO_WRF = {
    "wind_speed": ("U", "V"),        # 需要分解为 U/V 分量
    "u_component": ("U",),
    "v_component": ("V",),
    "temperature": ("T",),           # WRF T 是扰动位温，需加 T00(300K)
    "humidity": ("QVAPOR",),         # 水汽混合比 kg/kg
    "pressure": ("P", "PB"),         # P = 扰动气压, PB = 基础气压
    "geopotential": ("PH", "PHB"),   # PH = 扰动位势, PHB = 基础位势
}

# 从 WRF 变量名 -> 系统内部变量名 (反向映射)
WRF_TO_VARNAME = {
    "U": "u_component",
    "V": "v_component",
    "T": "temperature",
    "QVAPOR": "humidity",
    "P": "pressure",
    "PH": "geopotential",
}

T00 = 300.0  # WRF 参考温度 (K)


@dataclass
class GridMetadata:
    """WRF 网格元数据"""
    nx: int                    # 东西向格点数
    ny: int                    # 南北向格点数
    nz: int                    # 垂直层数
    dx: float                  # 格距(m)
    dy: float                  # 格距(m)
    xlat: Optional[np.ndarray] = None    # 纬度 (ny, nx)
    xlong: Optional[np.ndarray] = None   # 经度 (ny, nx)
    znu: Optional[np.ndarray] = None     # 垂直层 eta 值 (nz,)
    times: List[str] = field(default_factory=list)


@dataclass
class AnalysisField:
    """同化分析场 (来自 bayesian_assimilation.py 输出)"""
    variables: Dict[str, np.ndarray] = field(default_factory=dict)
    uncertainty: Dict[str, np.ndarray] = field(default_factory=dict)
    method: str = "hybrid"
    grid: Optional[GridMetadata] = None
    metadata: Dict = field(default_factory=dict)


class WrfInitializer:
    """将同化分析场写回 WRF 兼容格式，实现预报闭环"""

    def __init__(self, template_path: Optional[str] = None):
        """
        Args:
            template_path: WRF 原始 wrfinput_d0* 文件路径 (作为模板)
        """
        self.template_path = template_path
        self.grid = None

        if Dataset is None:
            raise ImportError("netCDF4 未安装。请执行: pip install netCDF4")

    # ============================================================
    # 模板读取
    # ============================================================

    def read_template(self, path: Optional[str] = None) -> GridMetadata:
        """从 WRF 模板文件读取网格元数据"""
        path = path or self.template_path
        if not path:
            raise ValueError("未指定 WRF 模板文件路径")

        if Dataset is None:
            raise ImportError("netCDF4 未安装")
        with Dataset(path, "r") as ds:
            we_stag = ds.dimensions.get("west_east_stag")
            sn_stag = ds.dimensions.get("south_north_stag")
            bt_stag = ds.dimensions.get("bottom_top_stag")
            we_stag_len = len(we_stag) if we_stag is not None else 0
            sn_stag_len = len(sn_stag) if sn_stag is not None else 0
            bt_stag_len = len(bt_stag) if bt_stag is not None else 0
            we_dim = ds.dimensions.get("west_east")
            sn_dim = ds.dimensions.get("south_north")
            bt_dim = ds.dimensions.get("bottom_top")
            self.grid = GridMetadata(
                nx=we_dim.size if we_dim is not None else (we_stag_len - 1),
                ny=sn_dim.size if sn_dim is not None else (sn_stag_len - 1),
                nz=bt_dim.size if bt_dim is not None else (bt_stag_len - 1),
                dx=float(getattr(ds, "DX", 0)),
                dy=float(getattr(ds, "DY", 0)),
            )
            if "XLAT" in ds.variables:
                self.grid.xlat = ds.variables["XLAT"][:].squeeze()
            if "XLONG" in ds.variables:
                self.grid.xlong = ds.variables["XLONG"][:].squeeze()
            if "ZNU" in ds.variables:
                self.grid.znu = ds.variables["ZNU"][:].squeeze()
            if "Times" in ds.variables:
                times = ds.variables["Times"][:]
                self.grid.times = [b"".join(t).decode("utf-8").strip() for t in times]
        return self.grid

    # ============================================================
    # 分析场 -> WRF 变量转换
    # ============================================================

    def analysis_to_wrf_variables(self, analysis: AnalysisField) -> Dict[str, np.ndarray]:
        """将分析场中的通用变量名转换为 WRF 原生变量名"""
        wrf_vars = {}
        for var_name, array in analysis.variables.items():
            if var_name in VARNAME_TO_WRF:
                wrf_names = VARNAME_TO_WRF[var_name]
                if var_name == "wind_speed":
                    u, v = self._wind_to_uv(array, analysis)
                    wrf_vars["U"] = u
                    wrf_vars["V"] = v
                elif var_name == "temperature":
                    wrf_vars["T"] = array - T00  # 温度 -> WRF 扰动位温
                else:
                    wrf_vars[wrf_names[0]] = array
            else:
                wrf_vars[var_name] = array
        return wrf_vars

    @staticmethod
    def _wind_to_uv(ws_array, analysis):
        """风速/风向 -> U/V 风分量 (需要风向数据)"""
        wd_array = analysis.variables.get(
            "wind_direction", analysis.variables.get("windDirection")
        )
        if wd_array is None:
            logger.warning("无风向数据，假设 WS=U, V=0")
            return ws_array, np.zeros_like(ws_array)
        theta = np.deg2rad(270 - wd_array)
        u = -ws_array * np.sin(theta)
        v = -ws_array * np.cos(theta)
        return u, v

    # ============================================================
    # 模式 A: REPLACE — 直接替换 wrfinput 中的变量
    # ============================================================

    def write_wrfinput(self, analysis: AnalysisField, output_path: str,
                       template_path: Optional[str] = None) -> bool:
        """
        以原始 wrfinput 为模板，替换被同化的变量值，生成新的初始场文件。

        WRF real.exe → wrfinput_d01 (模板)
        analysis field  → 替换 U/V/T/QVAPOR/... → wrfinput_d01_updated
        wrf.exe 使用更新后的初始场进行预报
        """
        template = template_path or self.template_path
        if not template:
            raise ValueError("未指定 WRF 模板文件路径")

        if not os.path.exists(template):
            logger.error(f"WRF 模板文件不存在: {template}")
            return False

        if Dataset is None:
            raise ImportError("netCDF4 未安装")
        try:
            wrf_vars = self.analysis_to_wrf_variables(analysis)

            with (Dataset(template, "r") as src,
                  Dataset(output_path, "w", format="NETCDF4") as dst):

                # 复制维度
                for name, dim in src.dimensions.items():
                    dst.createDimension(name, len(dim) if not dim.isunlimited() else None)

                # 复制变量，替换被同化的字段
                for var_name, var in src.variables.items():
                    if var_name in wrf_vars:
                        new_data = wrf_vars[var_name]
                        new_var = dst.createVariable(
                            var_name, var.datatype, var.dimensions,
                            zlib=True, complevel=4
                        )
                        new_var.setncatts({k: var.getncattr(k) for k in var.ncattrs()})
                        self._broadcast_and_assign(new_var, new_data, var[:].shape)
                        logger.info(f"  已替换变量: {var_name}, shape={new_data.shape}")
                    else:
                        new_var = dst.createVariable(
                            var_name, var.datatype, var.dimensions,
                            zlib=True, complevel=4
                        )
                        new_var.setncatts({k: var.getncattr(k) for k in var.ncattrs()})
                        new_var[:] = var[:]

                # 复制全局属性
                for attr in src.ncattrs():
                    if attr not in dst.ncattrs():
                        dst.setncattr(attr, src.getncattr(attr))

                dst.setncattr("TITLE", "WRF input with assimilated analysis field")
                dst.setncattr("ASSIMILATION_METHOD", analysis.method)

            logger.info(f"WRF 初始场已写入: {output_path}")
            return True

        except Exception as e:
            logger.error(f"WRF 初始场写入失败: {e}", exc_info=True)
            return False

    # ============================================================
    # 模式 B: NUDGING — 生成 grid nudging 文件
    # ============================================================

    def write_nudging_file(self, analysis: AnalysisField, output_path: str,
                           domain_start_time: str = "2024-01-01_00:00:00",
                           domain_end_time: str = "2024-01-01_06:00:00",
                           nudging_times: int = 7) -> bool:
        """
        生成 WRF analysis nudging 文件 (wrffdda_d0*)。

        WRF namelist 需配置:
          &fdda
           grid_fdda = 1
           gfdda_interval_m = 360
           gfdda_end_h = 6
           if_no_pbl_nudging_uv = 0
           if_no_pbl_nudging_t = 0
           if_no_pbl_nudging_q = 0
           guv = 0.0003
           gt = 0.0003
           gq = 0.0003
          /
        """
        if Dataset is None:
            raise ImportError("netCDF4 未安装")
        try:
            wrf_vars = self.analysis_to_wrf_variables(analysis)
            nt = nudging_times
            nz = self.grid.nz if self.grid else 1
            ny = self.grid.ny if self.grid else 1
            nx = self.grid.nx if self.grid else 1

            with Dataset(output_path, "w", format="NETCDF4") as ds:
                # 创建维度
                ds.createDimension("Time", None)
                ds.createDimension("DateStrLen", 19)
                ds.createDimension("west_east", nx)
                ds.createDimension("south_north", ny)
                ds.createDimension("bottom_top", nz)
                ds.createDimension("west_east_stag", nx + 1)
                ds.createDimension("south_north_stag", ny + 1)

                # 时间变量
                times = ds.createVariable("Times", "S1", ("Time", "DateStrLen"))
                times[:] = np.array([list(domain_start_time.ljust(19)) for _ in range(nt)])

                # Nudging 变量 (WT = tendency, not full field)
                for var_name, array in wrf_vars.items():
                    self._create_nudging_variable(ds, var_name, array, nz, ny, nx, nt)

                ds.setncattr("TITLE", "WRF FDDA analysis nudging file")
                ds.setncattr("ASSIMILATION_METHOD", analysis.method)

            logger.info(f"WRF nudging 文件已写入: {output_path}")
            return True

        except Exception as e:
            logger.error(f"WRF nudging 文件写入失败: {e}", exc_info=True)
            return False

    def _create_nudging_variable(self, ds, var_name, array, nz, ny, nx, nt):
        """在 NetCDF 文件中创建 nudging 变量 (带时间维度)"""
        if var_name == "QVAPOR":
            dims = ("Time", "bottom_top", "south_north", "west_east")
        elif var_name in ("U",):
            dims = ("Time", "bottom_top", "south_north", "west_east_stag")
        elif var_name in ("V",):
            dims = ("Time", "bottom_top", "south_north_stag", "west_east")
        else:
            dims = ("Time", "bottom_top", "south_north", "west_east")

        var = ds.createVariable(var_name, "f4", dims, zlib=True, complevel=4)
        # 广播到 (nt, nz, ny, nx)
        data_4d = np.zeros((nt, nz, ny, nx), dtype=np.float32)
        squeezed = array.squeeze()
        if squeezed.ndim == 3:
            data_4d[0] = squeezed.astype(np.float32)
        elif squeezed.ndim == 2:
            for k in range(nz):
                data_4d[0, k] = squeezed.astype(np.float32)
        elif squeezed.ndim == 1:
            data_4d[0, :, 0, 0] = squeezed.astype(np.float32)
        elif squeezed.ndim == 0:
            data_4d[:] = float(squeezed)
        for t in range(1, nt):
            data_4d[t] = data_4d[0]
        var[:] = data_4d

    # ============================================================
    # 辅助方法
    # ============================================================

    @staticmethod
    def _broadcast_and_assign(dst_var, new_data: np.ndarray, target_shape: tuple):
        """将分析场数据调整到目标 shape 并赋值"""
        data = new_data.squeeze()
        if data.shape == target_shape:
            dst_var[:] = data.astype(dst_var.datatype)
        elif len(data.shape) < len(target_shape):
            # 添加缺失维度
            for _ in range(len(target_shape) - len(data.shape)):
                data = data[np.newaxis, ...]
            # 按缺失维度广播
            for i, (dsz, tsz) in enumerate(zip(data.shape, target_shape)):
                if dsz != tsz and dsz == 1:
                    data = np.repeat(data, tsz, axis=i)
            dst_var[:] = data.astype(dst_var.datatype)
        else:
            # 截取匹配部分
            slices = tuple(slice(0, min(s, t)) for s, t in zip(data.shape, target_shape))
            dst_var[:] = data[slices].astype(dst_var.datatype)

    @staticmethod
    def load_analysis_from_json(filepath: str) -> AnalysisField:
        """从 bayesian_assimilation.py 输出的 JSON 文件加载分析场"""
        with open(filepath, "r", encoding="utf-8") as f:
            raw = json.load(f)
        data = raw.get("data", raw)

        variables = {}
        uncertainty = {}
        analysis_dict = data.get("analysis", data)
        for key, val in analysis_dict.items():
            if isinstance(val, list):
                variables[key] = np.array(val, dtype=np.float64)
            elif isinstance(val, dict) and key == "uncertainty":
                for uk, uv in val.items():
                    if isinstance(uv, list):
                        uncertainty[uk] = np.array(uv, dtype=np.float64)
        if "uncertainty" in data and isinstance(data["uncertainty"], dict):
            for uk, uv in data["uncertainty"].items():
                if isinstance(uv, list):
                    uncertainty[uk] = np.array(uv, dtype=np.float64)

        return AnalysisField(
            variables=variables,
            uncertainty=uncertainty,
            method=data.get("method", "hybrid"),
            metadata=data.get("metadata", {})
        )


# ============================================================
# CLI 入口
# ============================================================

def main():
    """
    用法:
      # 模式 REPLACE: 替换 wrfinput 初始场
      python wrf_initializer.py replace <analysis.json> <wrfinput_template> <output.nc>

      # 模式 NUDGING: 生成 nudging 文件
      python wrf_initializer.py nudging <analysis.json> <wrfinput_template> <output.nc>

      # 模式 INFO: 仅显示模板网格信息
      python wrf_initializer.py info <wrfinput_template>
    """
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    mode = sys.argv[1]
    init = WrfInitializer()

    if mode == "info":
        path = sys.argv[2] if len(sys.argv) > 2 else None
        if path:
            grid = init.read_template(path)
            logger.info(json.dumps({
                "nx": grid.nx, "ny": grid.ny, "nz": grid.nz,
                "dx": grid.dx, "dy": grid.dy,
                "lat_range": (
                    [float(grid.xlat.min()), float(grid.xlat.max())]
                    if grid.xlat is not None else None
                ),
                "lon_range": (
                    [float(grid.xlong.min()), float(grid.xlong.max())]
                    if grid.xlong is not None else None
                ),
            }, indent=2))

    elif mode == "replace":
        if len(sys.argv) < 5:
            logger.info(
                "用法: python wrf_initializer.py replace "
                "<analysis.json> <wrfinput.nc> <output.nc>"
            )
            sys.exit(1)
        analysis = WrfInitializer.load_analysis_from_json(sys.argv[2])
        init.read_template(sys.argv[3])
        success = init.write_wrfinput(analysis, sys.argv[4], sys.argv[3])
        sys.exit(0 if success else 1)

    elif mode == "nudging":
        if len(sys.argv) < 5:
            logger.info(
                "用法: python wrf_initializer.py nudging "
                "<analysis.json> <wrfinput.nc> <output.nc>"
            )
            sys.exit(1)
        analysis = WrfInitializer.load_analysis_from_json(sys.argv[2])
        init.read_template(sys.argv[3])
        success = init.write_nudging_file(analysis, sys.argv[4])
        sys.exit(0 if success else 1)

    else:
        print(f"未知模式: {mode}，支持: replace, nudging, info")
        sys.exit(1)


if __name__ == "__main__":
    main()
