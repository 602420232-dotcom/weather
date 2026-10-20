#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
WRF 低空气象解析器（重写版 v2，2026-09-24）
================================================================================

【为什么重写】
原 `wrf_processor.py` 结构是真的（真读 wrfout、真用 netCDF4），但有一条**硬契约错误**
和五处**物理错误**，且它通过 Java 路径**从未成功运行过**：

  1) ★★ CLI 契约不匹配（**这是"链路不可用"的真正原因**）
     `PythonScriptInvoker` 的调用形式是
         python <script>.py <action> <params_json_file>
     且它**校验 action 必须属于白名单**。同项目里写法正确的 `meteor_forecast.py` 就是
         command = sys.argv[1];  input_data = load_input(2)   # argv[2] 是 JSON 文件
     而原 `wrf_processor.py` 把 `argv[1]` 当作 **NetCDF 文件路径**：
         file_path = sys.argv[1]      # 实际拿到字符串 "parse"
     于是 `nc.Dataset("parse")` 必然失败 ⇒ 永远返回 `无法打开WRF文件`。
     **与 pythonScriptPath 配置、netCDF4 是否安装都无关。**

  2) 输出结构与前端毫无交集
     前端 `NetCDFPreview.vue` / `NetCDFChunkUploader.vue` 消费的 payload 形状是
         { fileName, fileSize, dims{time,level,lat,lon}, vars[],
           levelNames[], grid{lat,lon,levels}, temperature[L][Y][X], u, v }
     而原脚本输出 `{meteorological, turbulence, visibility, lightning_risk, ...}`。

  3) 五处物理错误（详见每处代码内的 ★FIX 注记）
     ★FIX1 QVAPOR 当相对湿度   ⇒ RH 恒为 100%，永远饱和
     ★FIX2 位温当温度 (T+300)  ⇒ 实测均值 333.5 K vs 真实 255.4 K，**差 78 K**
     ★FIX3 沿 Time 求平均当湍流脉动 ⇒ "TKE" 大 10~100 倍
     ★FIX4 CAPE/LCL 用无出处的系数 (80/200/5) 凑 ⇒ 已换成**真实气块抬升法**
     ★FIX5 把场压成时间序列、丢掉空间结构 ⇒ 现输出完整 [level][lat][lon]
     ★FIX6 完全没用 `W`                    ⇒ **部分修复，措辞已更正**（见下）
      ★FIX7  CIN 被 `clip(cin,-1000,0)` 截成恒 0（2026-09-25 修正）
      ★FIX8  LFC 的"未找到"按 0 m 参与平均 ⇒ 均值偏低（三列一有效时低 67%）
      ★FIX9  降水因子恒 0 却仍占 15% 权重（2026-09-25 修正）
      ★FIX10 Java 传的 `height` 被静默忽略（2026-09-25 修正）
      ★FIX11 `dims` 与 stride 之后的数组不一致（2026-09-25 修正）

  ★★ 关于 FIX6 的**诚实更正**（2026-09-25）：
     原措辞"W ⇒ 下沉/上升气流**与 TKE 诊断**"，**后半句与实现不符** ——
     `tke_mixing_length()` 用的是 k-l 一阶闭合的平衡估计
     （`TKE ≈ l²S²(1−Ri/Ri_c)`，只用 U/V/θ/z），**W 从未进入任何湍流量**。
     这本身不是缺陷：k-l 闭合的 TKE 由**切变与层结**决定，本来就不需要 w。
     ⇒ 已把 W 用在它**真正该用的地方**（上升/下沉气流极值与垂直速度统计，
       放进 `risk.turbulence`），并把注释改成与实现一致。
     **"注释说用了"≠"真的用了"** —— 本项目已因此栽过（"文件里有≠会被用上"）。

【本项目真实 wrfout 的实测约束（成都 3 km，2026-09-24 勘察）】
  · 维度: Time=1, bottom_top=33(stag 34), south_north=129, west_east=149(stag 150)
  · 有:   U V W T P PB PH PHB QVAPOR Q2 T2 PSFC U10 V10 HGT XLAT XLONG PBLH UST HFX QCLOUD ZNU ZNW
  · **没有**: QKE / TKE / CAPE / CIN / LCL （`bl_pbl_physics=1` YSU 不输出 QKE）
  ⇒ ★ **原脚本"优先用 WRF 原生诊断量"的分支是【死代码】，必定落进那三个编造的兜底公式。**
    所以本版把湍流与对流有效位能**按物理正解实现**，不再叫"兜底"。
  · 第 1 层高度 21.8~26.2 m AGL（低层近均匀）⇒ 垂直插值到固定 AGL 是良态的
  · 地形 248~5213 m

【契约（本文件对外承诺）】
  调用:   python wrf_processor.py <action> <params_json_file>
  action ∈ {"parse", "variance", "validate", "variables", "transform", "compute"}
          （必须落在 `PythonScriptInvoker.ALLOWED_ACTIONS` 内，否则 Java 侧直接拒绝）
  params: {"filePath": "...", "height": 100, "paramType": "all",
           "levels": [...], "stride": 1, "paVariancePath": "..."(可选)}
  输出:   **单个 JSON 对象，print 到 stdout**（Java 用 redirectErrorStream(true) 合并 stderr，
          但正确做法是写 stdout；日志一律走 stderr，避免污染）
  大小:   受 `PythonScriptInvoker.MAX_SCRIPT_SIZE_BYTES = 10 MB` 限制
          ⇒ 内置 auto_stride 护栏，估算超限自动加大 stride 并在 meta 里说明

【文献出处（不用无来源的系数）】
  · 位温→温度:     标准 Poisson 关系，R_d/cp = 287.0/1004.5（WRF 默认常数）
  · 饱和水汽压:    Bolton (1980, MWR) 式(10)
  · 露点/LCL:      Bolton (1980) 式(11)(15)
  · 湿绝热递减率:  伪绝热近似（AMS Glossary / Bakhshaii & Stull 2013 综述）
  · TKE:           k-l 一阶闭合的平衡估计 TKE ≈ l²S²(1 − Ri/Ri_c)，
                   l 用 Blackadar 形式，Ri_c = 0.25（Stull 1988; Jacobson 2005）
  · 能见度:        Stoelinga & Warner (1999) 的 RH–能见度经验关系（WRF 社区常用），
                   ★ 这是**参数化**，不是第一性原理 —— 论文里必须如此表述
  · 风险评分口径:  沿用本项目 `uav-platform-v2/.../risk/weather_risk.py` 的约定
                   （risk_score / risk_level ∈ low|medium|high|critical / factors）
                   而不是 Java mock 的 LOW|MODERATE|HIGH|EXTREME
"""

from __future__ import annotations

import json
import logging
import math
import os
import sys
from typing import Any, Dict, List, Optional, Tuple

import netCDF4 as nc
import numpy as np

# ─────────────────────────────────────────────────────────────────────────────
#  日志：一律写 stderr，stdout 只留给 JSON
#  ★ 原脚本用 logging.basicConfig() 默认写 stderr + logger.info(json)，
#    靠 Java 的 redirectErrorStream(true) 才"碰巧"能读到。这里改正确。
# ─────────────────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    stream=sys.stderr,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger("wrf_processor")


# ═════════════════════════════════════════════════════════════════════════════
#  1. 物理常数（与 WRF 默认一致，来源明确）
# ═════════════════════════════════════════════════════════════════════════════
G = 9.80665            # m s-2
RD = 287.0             # J kg-1 K-1   干空气气体常数
CP = 1004.5            # J kg-1 K-1   定压比热
P0_DEFAULT = 100000.0  # Pa           参考气压（wrfout 有 P00 时以它为准）
EPS = 0.622            # Rd/Rv
LV = 2.501e6           # J kg-1       汽化潜热（0 °C）
KARMAN = 0.40          # von Kármán 常数
RD_OVER_CP = RD / CP   # 0.28571      Poisson 指数
RI_CRIT = 0.25         # 临界梯度 Richardson 数
L_INF_M = 100.0        # Blackadar 混合长上界（m）
VIS_RH0 = 0.80         # Stoelinga & Warner (1999) 的 RH 阈值

# 目标高度层（m AGL）。2 m 用 T2/Q2，10 m 用 U10/V10，其余垂直插值。
DEFAULT_LEVELS_M: List[float] = [2, 10, 50, 100, 200, 300, 500, 800, 1000, 1500, 2000, 3000]

MAX_OUT_BYTES = 9 * 1024 * 1024     # 留 1 MB 余量给 Java 的 10 MB 上限
# ★ 实际输出的**大数组个数**。auto_stride 必须用真实值估算 ——
#   首版按 3（只数了 temperature/u/v）估算，结果 129×149×12 的域算出 5.3 MB、
#   以为安全，**实际输出 18.2 MB**，超过 Java 上限 ⇒ 会被
#   `PythonScriptInvoker` 的 `break` **静默截断**，前端拿到半个 JSON。
#   计数：payload 7 个（temperature/u/v/rh/w/wind_speed/pressure_hpa）
#         + risk.turbulence.level_field + risk.visibility.level_field = 9
N_ARRAYS = 9
BYTES_PER_VALUE = 9.0               # 实测 ≈8.8（含逗号、两位小数、负号）
ALLOWED_ACTIONS = {
    "parse", "variance", "validate", "variables", "transform", "compute",
    "execute", "batch",
}


# ═════════════════════════════════════════════════════════════════════════════
#  2. 小工具
# ═════════════════════════════════════════════════════════════════════════════
def _r(x: Any, nd: int = 2) -> Optional[float]:
    """转成 JSON 友好的 float；NaN/Inf → None（JSON 不允许它们）。"""
    try:
        v = float(x)
    except (TypeError, ValueError):
        return None
    if math.isnan(v) or math.isinf(v):
        return None
    return round(v, nd)


def _arr2list(a: np.ndarray, nd: int = 2) -> List:
    """ndarray → 嵌套 list，NaN→None，保留形状（★FIX5：不压成时间序列）。"""
    a = np.asarray(a, dtype=np.float64)
    out = np.where(np.isfinite(a), np.round(a, nd), np.nan)
    def conv(x):
        if isinstance(x, np.ndarray):
            return [conv(i) for i in x]
        f = float(x)
        return None if math.isnan(f) else f
    return conv(out)


def _es_hpa(T: np.ndarray) -> np.ndarray:
    """饱和水汽压 (hPa)。Bolton (1980, MWR) 式(10)，T 单位 K。"""
    Tc = T - 273.15
    return 6.112 * np.exp(17.67 * Tc / (Tc + 243.5))


def _qs(p_pa: np.ndarray, T: np.ndarray) -> np.ndarray:
    """饱和比湿 (kg/kg)。"""
    es = _es_hpa(T) * 100.0
    return EPS * es / np.maximum(p_pa - (1.0 - EPS) * es, 1.0)


def _dewpoint_k(p_pa: np.ndarray, q: np.ndarray) -> np.ndarray:
    """比湿 → 露点 (K)。Bolton (1980) 式(11) 反解。"""
    e = np.maximum(q * p_pa / (EPS + q), 1e-6)
    ln = np.log(e / 611.2)
    return 243.5 * ln / (17.67 - ln) + 273.15


def _tlcl_k(T: np.ndarray, Td: np.ndarray) -> np.ndarray:
    """抬升凝结温度 (K)。Bolton (1980) 式(15)。"""
    return 1.0 / (1.0 / (Td - 56.0) + np.log(T / Td) / 800.0) + 56.0


def _dTdp_moist(T: np.ndarray, p: np.ndarray, qs: np.ndarray) -> np.ndarray:
    """伪绝热抬升的 dT/dp (K/Pa)。"""
    num = RD * T + LV * qs
    den = p * (CP + (LV * LV * qs * EPS) / (RD * T * T))
    return num / den


def _virtual(T: np.ndarray, q: np.ndarray) -> np.ndarray:
    """虚温 (K)：Tv = T(1 + 0.61 q)。"""
    return T * (1.0 + 0.61 * q)


# ═════════════════════════════════════════════════════════════════════════════
#  3. 读取器
# ═════════════════════════════════════════════════════════════════════════════
class WrfReader:
    """按需读取 wrfout 并计算低空诊断量。

    所有 3-D 场统一为 **层中心** 形状 (nz, ny, nx)：
      U (west_east_stag)  → 在 x 方向去交错
      V (south_north_stag) → 在 y 方向去交错
      W, PH/PHB (bottom_top_stag) → 在 z 方向去交错
    """

    def __init__(self, path: str, time_index: int = 0):
        self.path = path
        self.ti = time_index
        self.ds: Optional[nc.Dataset] = None
        self.notes: List[str] = []
        self._cache: Dict[str, np.ndarray] = {}
        self._theta_base: Optional[float] = None
        self._theta_base_note: str = ""
        # ★FIX12：位温基准的**自证证据**（verified / residual / 候选集）。
        #   下游据此判断"这个温度场能不能直接用于结论"。
        self.theta_base_evidence: Dict[str, Any] = {}

    # ── 生命周期 ──────────────────────────────────────────────────────────
    def open(self) -> bool:
        try:
            self.ds = nc.Dataset(self.path, "r")
        except Exception as e:                       # noqa: BLE001
            logger.error("无法打开 %s: %s", self.path, e)
            return False
        if not self.ds.variables:
            logger.error("文件里没有变量: %s", self.path)
            return False
        return True

    def close(self) -> None:
        if self.ds is not None:
            try:
                self.ds.close()
            finally:
                self.ds = None

    def has(self, name: str) -> bool:
        return self.ds is not None and name in self.ds.variables

    def _get(self, name: str) -> np.ndarray:
        """读一个变量并去掉 Time 维，结果 (…)。带缓存。"""
        if name in self._cache:
            return self._cache[name]
        var = self.ds.variables[name]
        a = np.asarray(var[self.ti] if var.ndim >= 1 and "Time" in var.dimensions
                       else var[:], dtype=np.float64)
        self._cache[name] = a
        return a

    # ── 基本几何 ──────────────────────────────────────────────────────────
    @property
    def shape3d(self) -> Tuple[int, int, int]:
        return self._get("T").shape if self.has("T") else (0, 0, 0)

    def latlon(self) -> Tuple[np.ndarray, np.ndarray]:
        lat = self._get("XLAT") if self.has("XLAT") else None
        lon = self._get("XLONG") if self.has("XLONG") else None
        if lat is None or lon is None:
            return np.array([]), np.array([])
        return lat, lon

    # ── 压力 / 位温 / 温度 ────────────────────────────────────────────────
    def pressure_pa(self) -> np.ndarray:
        """全气压 p = P + PB (Pa)。"""
        return self._get("P") + self._get("PB")

    def p0(self) -> float:
        if self.has("P00"):
            try:
                return float(np.asarray(self.ds.variables["P00"][:]).ravel()[0])
            except Exception:                        # noqa: BLE001
                pass
        return P0_DEFAULT

    def theta_base(self) -> Tuple[float, str]:
        """判定 θ = T + base 该用哪个 base —— **用文件自带的 TH2 做数据自证**。

        ★★ 为什么不能直接用文件里的 `T00`（本版首稿就是在这里栽的）
        ------------------------------------------------------------
        本域 wrfout 写着 `T00 = 290`（源自 namelist 的 `base_temp = 290.0`），
        但**实测表明 `T` 场的扰动参考是 300**：

            base=300  ⇒  θ_L1 − TH2 的均值 = −0.48 K（max 1.18 K）   ✅
            base=290  ⇒  θ_L1 − TH2 的均值 = −10.48 K（max 11.18 K） ❌

        用 290 会让**整层环境温度系统性偏低 10 K**，而气块用的是真实 `T2`
        （不经过这个转换）⇒ 环境被算冷 ⇒ 气块相对偏暖 10 K
        ⇒ **CAPE 虚高到 6049 J/kg，且空间型完全反了（高原高、盆地低）**。

        根因：wrfout 里的 `T00` 是**静力基态常数**（来自 `base_temp`），
        而 `th_phy = T + t00` 里的 `t00` 是**另一个常数**（WRF 标准值 300）。
        两者同名但不同物。
        ⇒ 与本项目"`GEOGRID.TBL` 里有 VAR_SSO ≠ 它填了值"是同一类问题：
          **文件里有 `T00` ≠ `T00` 是 `T` 场的正确参考。**

        ⚠️ **原版 v1 硬编码 `T + 300` 在这一点上是对的**，
           我"改成读 T00"反而引入了回归。这里改成**数据自证**，
           任何 WRF 构建下都能自动选对，并把选择依据写进 meta 供审计。
        """
        cands: List[Tuple[float, str]] = [(300.0, "WRF 标准 t00")]
        if self.has("T00"):
            try:
                v = float(np.asarray(self.ds.variables["T00"][:]).ravel()[0])
                if abs(v - 300.0) > 0.5:
                    cands.append((v, "文件 T00(静力基态)"))
            except Exception:                          # noqa: BLE001
                pass

        if not self.has("TH2"):
            # ★FIX12：自证**做不了**时必须**留痕**，不能悄悄退回默认值。
            #   旧版只说一句 note，`success` 照样是 true ⇒ 下游不知道
            #   "这个 base 是自证出来的"还是"没证据、拿的默认值"。
            self.theta_base_evidence = {
                "verified": False,
                "reason": "文件里没有 TH2，无法做数据自证",
                "base_used": 300.0}
            self.notes.append(
                "⚠️ 位温基准**未能自证**（缺 TH2）⇒ 用了 WRF 标准 300。"
                "若该文件实际用的是别的 base_temp，温度会整体偏移")
            return 300.0, "无 TH2 可自证 ⇒ 用 WRF 标准 300（**未自证**）"
        th2 = self._get("TH2")
        T0 = self._get("T")[0]
        best, best_err, best_note = 300.0, float("inf"), ""
        detail = []
        for b, why in cands:
            err = float(np.median(np.abs((T0 + b) - th2)))
            detail.append(f"base={b:g}({why}):median|θ_L1−TH2|={err:.3f}K")
            if err < best_err:
                best, best_err, best_note = b, err, detail[-1]

        # ★FIX12：**候选集兜底风险** —— 若真实 base 不在候选集里，
        #   旧版会"选一个误差最小的"然后照样报 success，**静默选错**。
        #   判据：TH2 是 2 m 诊断量、θ_L1 在第 1 层中心（本域 21.8~26.2 m AGL），
        #   二者不等价，正常偏差约 −0.5 K。所以**残差超过 2 K 就说明
        #   候选集里没有正确的那一个**，此时必须报出来。
        #   （阈值 2 K 的依据：实测最优候选 −0.48 K，次优 −10.48 K，中间很空。）
        tol = 2.0
        verified = best_err <= tol
        self.theta_base_evidence = {
            "verified": bool(verified),
            "residual_K": round(best_err, 3),
            "tolerance_K": tol,
            "base_used": float(best),
            "candidates": [c[0] for c in cands],
            "note": best_note,
        }
        if not verified:
            self.notes.append(
                "⚠️ 位温基准**自证失败**：最优候选 %g K 的残差 %.3f K 超过阈值 %.1f K "
                "⇒ 真实基准很可能**不在候选集 %s 里**，温度场会有系统性偏移。"
                "**这条结果不应直接用于结论。**"
                % (best, best_err, tol, [c[0] for c in cands]))
        return best, ("自证选定 " + best_note + "；候选 " + " | ".join(detail))

    def theta_k(self) -> np.ndarray:
        """位温 θ = T + base。base 由 `theta_base()` 数据自证选定。"""
        if self._theta_base is None:
            b, note = self.theta_base()
            self._theta_base, self._theta_base_note = b, note
            self.notes.append(f"位温基准 {b:g} K（{note}）")
        return self._get("T") + self._theta_base

    def temperature_k(self) -> np.ndarray:
        """★FIX2 位温 → 实际温度：T = θ (p/p0)^(Rd/cp)。

        原实现用 `np.mean(T) + 300`，得到的是**位温**不是温度。
        实测（成都域）：θ 均值 333.5 K，真实温度均值 255.4 K —— **差 78 K**。
        拿 θ 代进 Magnus 公式会严重高估饱和水汽压。
        """
        return self.theta_k() * (self.pressure_pa() / self.p0()) ** RD_OVER_CP

    # ── 高度（AGL）────────────────────────────────────────────────────────
    def height_agl(self) -> np.ndarray:
        """用 PH/PHB 算层中心 AGL 高度 (m)，形状 (nz, ny, nx)。

        PH/PHB 是**位势**（m² s⁻²）在 bottom_top_stag 上，必须除以 g 再取层中心。
        低空 eta 面随地形起伏，所以不能用"eta × 常数"近似。
        """
        z_stag = (self._get("PH") + self._get("PHB")) / G      # (nz+1, ny, nx)
        z = 0.5 * (z_stag[:-1] + z_stag[1:])                   # 层中心
        hgt = self._get("HGT") if self.has("HGT") else np.zeros(z.shape[1:])
        return z - hgt[None, :, :]

    # ── 风 ────────────────────────────────────────────────────────────────
    def wind_uv(self) -> Tuple[np.ndarray, np.ndarray]:
        """去交错后的 U, V，均为 (nz, ny, nx)。"""
        U = self._get("U")
        if U.shape[-1] > 1:                       # west_east_stag
            U = 0.5 * (U[..., :-1] + U[..., 1:])
        V = self._get("V")
        if V.shape[-2] > 1:                       # south_north_stag
            V = 0.5 * (V[..., :-1, :] + V[..., 1:, :])
        return U, V

    def wind_w(self) -> Optional[np.ndarray]:
        """★FIX6 去交错后的 W (nz, ny, nx)。原实现完全没用 W。"""
        if not self.has("W"):
            self.notes.append("wrfout 无 W，跳过垂直速度诊断")
            return None
        W = self._get("W")
        if W.shape[0] > 1:                        # bottom_top_stag
            W = 0.5 * (W[:-1] + W[1:])
        return W

    def wind_speed(self) -> np.ndarray:
        U, V = self.wind_uv()
        return np.sqrt(U * U + V * V)

    # ── 湿度 ──────────────────────────────────────────────────────────────
    def qvapor(self) -> np.ndarray:
        """比湿 (kg/kg)。"""
        return self._get("QVAPOR")

    def rh_percent(self) -> np.ndarray:
        """★FIX1 真实相对湿度 (%)。

        原实现 `humidity = np.mean(QVAPOR) * 100` —— QVAPOR 是 **kg/kg 比湿**，
        典型值 0.01 ⇒ ×100 得 1.0 ⇒ 再 ×100 判 RH=100% ⇒ **恒为饱和**。
        正确做法：RH = 100 · q / q_s(p, T)，其中 q_s 用 Bolton 饱和水汽压。
        """
        q = self.qvapor()
        p = self.pressure_pa()
        T = self.temperature_k()
        qs = np.maximum(_qs(p, T), 1e-12)
        return np.clip(100.0 * q / qs, 0.0, 100.0)

    # ── 近地面 ────────────────────────────────────────────────────────────
    def surface(self) -> Dict[str, np.ndarray]:
        """2 m / 10 m 诊断量。WRF 只提供 2 m 温湿与 10 m 风，没有 2 m 风。"""
        out: Dict[str, np.ndarray] = {}
        for k in ("T2", "Q2", "U10", "V10", "PSFC", "HGT", "PBLH", "UST", "HFX"):
            if self.has(k):
                out[k] = self._get(k)
        return out

    def surface_wind_speed(self) -> Optional[np.ndarray]:
        s = self.surface()
        if "U10" in s and "V10" in s:
            return np.sqrt(s["U10"] ** 2 + s["V10"] ** 2)
        return None

    # ── 垂直插值 ──────────────────────────────────────────────────────────
    @staticmethod
    def interp_to_levels(field: np.ndarray, z_agl: np.ndarray,
                         targets: List[float],
                         anchor_val: Optional[np.ndarray] = None,
                         anchor_z: Optional[float] = None) -> np.ndarray:
        """把 (nz,ny,nx) 的场沿垂直插值到 targets (m AGL)，返回 (nt,ny,nx)。

        anchor_val/anchor_z：可选的近地面锚点（如 T2 在 2 m、U10 在 10 m）。
        低于最低锚点的目标层取最低锚点值（**不外推**，并在 meta 里说明）。
        """
        f = field
        z = z_agl
        if anchor_val is not None and anchor_z is not None:
            f = np.concatenate([anchor_val[None, :, :], f], axis=0)
            z = np.concatenate([np.full_like(anchor_val, float(anchor_z))[None, :, :], z], axis=0)

        nz = f.shape[0]
        out = np.empty((len(targets),) + f.shape[1:], dtype=np.float64)
        for ti, ht in enumerate(targets):
            k = np.sum(z <= ht, axis=0) - 1                 # (ny,nx)
            k = np.clip(k, 0, nz - 2)
            kk = k[None, :, :]
            z0 = np.take_along_axis(z, kk, axis=0)[0]
            z1 = np.take_along_axis(z, kk + 1, axis=0)[0]
            f0 = np.take_along_axis(f, kk, axis=0)[0]
            f1 = np.take_along_axis(f, kk + 1, axis=0)[0]
            dz = z1 - z0
            w = np.where(dz > 1e-6, (ht - z0) / np.maximum(dz, 1e-6), 0.0)
            out[ti] = f0 + w * (f1 - f0)
        return out

    # ═════════════════════════════════════════════════════════════════════
    #  诊断量
    # ═════════════════════════════════════════════════════════════════════
    def tke_mixing_length(self) -> Tuple[np.ndarray, np.ndarray]:
        """★FIX3 湍流动能 TKE (m² s⁻²) 与耗散率 ε (m² s⁻³)。

        **原实现为什么错**：`U_mean = np.mean(U, axis=0)` 是沿 **Time** 求平均，
        得到的 `u'` 是**时间偏差**（日变化/天气尺度），不是湍流脉动 ⇒ "TKE" 大 10~100 倍。
        而且单时次 wrfout 根本没有时间维可用来定义脉动。

        **本版做法**：既然 WRF 未输出 QKE/TKE（`bl_pbl_physics=1`），
        就用 **k-l 一阶闭合的平衡 TKE** 估计，只用 U/V/θ/z 这些真实存在的量：

            S²  = (∂u/∂z)² + (∂v/∂z)²           风切变平方
            N²  = (g/θ)(∂θ/∂z)                  浮力频率平方
            Ri  = N²/S²                         梯度 Richardson 数
            l   = κz / (1 + κz/L∞)              Blackadar 混合长，L∞ = 100 m
            TKE ≈ l² S² max(0, 1 − Ri/Ri_c)     Ri_c = 0.25
            ε   ≈ TKE^1.5 / l                   标准量纲估计

        量级自检：z=100 m ⇒ l≈28.6 m；S=0.05 s⁻¹ ⇒ TKE≈2.0 m² s⁻²（合理）。
        ⚠️ **这是参数化估计，不是模式诊断量**，论文里必须如此表述。
        """
        U, V = self.wind_uv()
        theta = self.theta_k()
        z = self.height_agl()

        # 层间差分（用相邻层中心）
        dz = np.diff(z, axis=0)
        dz = np.where(np.abs(dz) < 1e-3, np.nan, dz)
        dUdz = np.diff(U, axis=0) / dz
        dVdz = np.diff(V, axis=0) / dz
        dthdz = np.diff(theta, axis=0) / dz

        S2 = dUdz ** 2 + dVdz ** 2
        theta_mid = 0.5 * (theta[:-1] + theta[1:])
        N2 = (G / np.maximum(theta_mid, 1.0)) * dthdz
        z_mid = 0.5 * (z[:-1] + z[1:])

        with np.errstate(divide="ignore", invalid="ignore"):
            Ri = np.where(S2 > 1e-12, N2 / np.maximum(S2, 1e-12), 10.0)
        Ri = np.clip(Ri, -10.0, 10.0)

        l = KARMAN * np.maximum(z_mid, 1.0)
        l = l / (1.0 + l / L_INF_M)

        tke_mid = (l ** 2) * S2 * np.maximum(0.0, 1.0 - Ri / RI_CRIT)
        tke_mid = np.clip(np.nan_to_num(tke_mid, nan=0.0), 0.0, 50.0)

        with np.errstate(divide="ignore", invalid="ignore"):
            eps_mid = np.where(l > 1e-6, tke_mid ** 1.5 / np.maximum(l, 1e-6), 0.0)
        eps_mid = np.nan_to_num(eps_mid, nan=0.0)

        # 补一层到 nz（最低层用第 1 个中间层值）
        tke = np.concatenate([tke_mid[:1], tke_mid], axis=0)
        eps = np.concatenate([eps_mid[:1], eps_mid], axis=0)
        return tke, eps

    def richardson_number(self) -> np.ndarray:
        """梯度 Richardson 数（层中心，补到 nz）。"""
        theta = self.theta_k()
        U, V = self.wind_uv()
        z = self.height_agl()
        dz = np.diff(z, axis=0)
        dz = np.where(np.abs(dz) < 1e-3, np.nan, dz)
        S2 = (np.diff(U, axis=0) / dz) ** 2 + (np.diff(V, axis=0) / dz) ** 2
        theta_mid = 0.5 * (theta[:-1] + theta[1:])
        N2 = (G / np.maximum(theta_mid, 1.0)) * (np.diff(theta, axis=0) / dz)
        with np.errstate(divide="ignore", invalid="ignore"):
            Ri = np.where(S2 > 1e-12, N2 / np.maximum(S2, 1e-12), 10.0)
        Ri = np.clip(np.nan_to_num(Ri, nan=10.0), -10.0, 10.0)
        return np.concatenate([Ri[:1], Ri], axis=0)

    def parcel_cape_cin(self) -> Tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
        """★FIX4 **真实气块抬升法**算 CAPE / CIN / LCL / LFC。

        原实现 `cape = max(0,(T_mean−290)*80 + Q_mean*200)`、`lcl = (20+(T−273.15)/5)*100`
        —— 系数 80/200/5 **没有任何出处**，而且用**全域平均位温**估 CAPE 本身不成立
        （CAPE 是气块对环境的**积分**，不是平均温度的函数）。

        本版按标准做法：地面气块（T2/Q2/PSFC）→ 干绝热抬升到 LCL → 湿绝热继续抬升，
        沿模式层积分浮力：
            CAPE = Rd ∫ (Tv_p − Tv_e) d ln p   （正浮力段）
            CIN  = Rd ∫ (Tv_e − Tv_p) d ln p   （LFC 以下的负浮力段）

        返回 (cape, cin, lcl_m, lfc_m)，均为 (ny,nx)。
        输出单位：CAPE/CIN 为 J kg⁻¹，LCL/LFC 为 m AGL。
        """
        surf = self.surface()
        ny, nx = self._get("T").shape[1:]
        zeros = np.zeros((ny, nx), dtype=np.float64)

        if not all(k in surf for k in ("PSFC", "T2", "Q2")):
            self.notes.append("缺少 PSFC/T2/Q2，无法做气块抬升 ⇒ CAPE/CIN 返回 0")
            return zeros, zeros, zeros, zeros

        p_sfc = surf["PSFC"]
        T_sfc = surf["T2"]
        q_sfc = np.maximum(surf["Q2"], 1e-6)
        hgt = surf.get("HGT", zeros)

        p_lev = self.pressure_pa()
        T_lev = self.temperature_k()
        q_lev = self.qvapor()
        z_agl = self.height_agl()

        Td = _dewpoint_k(p_sfc, q_sfc)
        Tlcl = _tlcl_k(T_sfc, Td)
        with np.errstate(invalid="ignore"):
            p_lcl = p_sfc * (Tlcl / T_sfc) ** (1.0 / RD_OVER_CP)

        cape = np.zeros((ny, nx), dtype=np.float64)
        cin = np.zeros((ny, nx), dtype=np.float64)
        lfc_h = np.zeros((ny, nx), dtype=np.float64)
        found_lfc = np.zeros((ny, nx), dtype=bool)

        Tp = T_sfc.copy()          # 气块温度，随抬升更新
        p_prev = p_sfc.copy()
        nz = p_lev.shape[0]

        for k in range(nz):
            p_k = p_lev[k]
            dp = p_k - p_prev                     # < 0（上升）
            above_lcl = p_k <= p_lcl

            # 干绝热：T_p = T_sfc (p/p_sfc)^(Rd/cp)
            Tp_dry = T_sfc * (p_k / p_sfc) ** RD_OVER_CP
            # 湿绝热：从当前 Tp 沿伪绝热积分到 p_k。
            # ★ 子步数从 4 提到 16：一阶 Euler 的误差 ∝ 1/n，
            #   `validate_physics.py` 的 A5' 实测 nsub=50 与 2000 相差 0.21 K
            #   ⇒ 4 子步/层 × 33 层 ≈ 132 子步时误差约 0.08 K（折算 CAPE ≈0.5%）。
            #   提到 16 ⇒ ≈528 子步，误差约 0.02 K。代价可忽略（向量化，仍 < 1 s）。
            NSUB = 16
            Tp_wet = Tp.copy()
            for s in range(NSUB):
                p_a = p_prev + dp * (s / NSUB)
                p_b = p_prev + dp * ((s + 1) / NSUB)
                p_mid = 0.5 * (p_a + p_b)
                qs_mid = _qs(p_mid, Tp_wet)
                Tp_wet = Tp_wet + _dTdp_moist(Tp_wet, p_mid, qs_mid) * (p_b - p_a)
            Tp_new = np.where(above_lcl, Tp_wet, Tp_dry)

            # 平均气块温度（本层与上一层）
            Tp_bar = 0.5 * (Tp_new + Tp)
            Tv_p = _virtual(Tp_bar, np.where(above_lcl, _qs(p_k, Tp_bar), q_sfc))
            Tv_e = _virtual(0.5 * (T_lev[k] + (T_lev[k - 1] if k > 0 else T_sfc)),
                            0.5 * (q_lev[k] + (q_lev[k - 1] if k > 0 else q_sfc)))

            # ★ 浮力积分：CAPE = Rd ∫ (Tv_p − Tv_e) d(−ln p)
            #   ⚠️ 符号坑（本版首稿写反过）：上升时 p 减小 ⇒ ln(p_k/p_prev) < 0，
            #   直接乘它会把"气块偏暖"算成负浮力，**把 CAPE 与 CIN 完全颠倒**。
            #   正确写法用 ln(p_prev/p_k) > 0。
            #   这个错误**普通区间检查抓不到**（0 ≤ CAPE ≤ 8000 照样通过），
            #   必须用"CAPE 应与地面温度正相关"这类**符号敏感**的判据才能发现。
            dlnp_up = np.log(np.maximum(p_prev, 1.0) / np.maximum(p_k, 1.0))
            buoy = RD * (Tv_p - Tv_e) * dlnp_up      # J kg-1，正=正浮力

            pos = buoy > 0
            cape += np.where(pos, buoy, 0.0)
            # CIN 只累积到 LFC 之前
            cin += np.where((~pos) & (~found_lfc), -buoy, 0.0)
            newly = pos & (~found_lfc)
            if np.any(newly):
                lfc_h = np.where(newly, z_agl[k], lfc_h)
                found_lfc = found_lfc | newly

            Tp = Tp_new
            p_prev = p_k

        cape = np.clip(cape, 0.0, 10000.0)
        # ★FIX-CIN（2026-09-25 修正）：上面 L593 累加的是**抑制能量的量级**
        #   （写的是 `-buoy`，而 buoy<0 ⇒ -buoy>0），所以 `cin` 到这里是**正值**。
        #   旧版写 `np.clip(cin, -1000.0, 0.0)` ⇒ **把所有正值截成 0** ⇒ `cin_min` 恒为 0。
        #
        #   ★★ 这个 bug 能活下来，是因为自检里那条判据是 `cin_min <= 0` ——
        #      **测试和代码犯了同一个错**：恒 0 永远满足 `<= 0`，全绿。
        #      （与 `validate_physics.py` 开头记录的"区间检查抓不到符号错误"是同一类。）
        #
        #   正确做法：**保留量级、补上负号**（WRF 的 CIN 约定就是 ≤ 0），
        #   而不是把量级丢掉。修完两条判据同时成立：`cin ≤ 0` **且** `|cin|` 有量级。
        cin = -np.clip(cin, 0.0, 1000.0)

        # LCL 高度：在每列里找 p_lcl 落在哪两层之间，线性插值得到 AGL 高度。
        # （首稿这里留了三行互相覆盖的死代码，已清掉。）
        lcl_m = np.zeros((ny, nx), dtype=np.float64)
        for k in range(nz):
            if k == 0:
                m = p_lcl >= p_lev[0]
                lcl_m = np.where(m, z_agl[0], lcl_m)
            else:
                m = (p_lcl < p_lev[k - 1]) & (p_lcl >= p_lev[k])
                if np.any(m):
                    p0_, p1_ = p_lev[k - 1], p_lev[k]
                    z0, z1 = z_agl[k - 1], z_agl[k]
                    frac = np.where(np.abs(p0_ - p1_) > 1e-6,
                                    (p_lcl - p0_) / np.maximum(p0_ - p1_, 1e-6), 0.0)
                    lcl_m = np.where(m, z0 + frac * (z1 - z0), lcl_m)

        return cape, cin, np.nan_to_num(lcl_m), np.nan_to_num(lfc_h)

    # ── 能见度 ────────────────────────────────────────────────────────────
    def visibility_m(self) -> Tuple[np.ndarray, np.ndarray]:
        """★FIX1/2 能见度 (m) 与消光系数 (km⁻¹)。

        用 Stoelinga & Warner (1999) 的 RH–能见度经验关系（WRF 社区常用）：
            Vis[km] = (1/RH − 1) / (0.3 (1/RH0 − 1)),   RH0 = 0.80
        再按云水 QCLOUD 做雾/云内限幅。**这是参数化，不是第一性原理** ——
        论文里必须写明。RH 本身用 ★FIX1 修正后的真实值。
        """
        rh = self.rh_percent() / 100.0            # 0~1
        vis_km = np.full_like(rh, 30.0)
        mask = rh > VIS_RH0
        with np.errstate(divide="ignore", invalid="ignore"):
            v = (1.0 / np.maximum(rh, 1e-6) - 1.0) / (0.3 * (1.0 / VIS_RH0 - 1.0))
        vis_km = np.where(mask, np.clip(v, 0.05, 30.0), 30.0)

        if self.has("QCLOUD"):
            qc = np.maximum(self._get("QCLOUD"), 0.0)
            rho = self.pressure_pa() / (RD * np.maximum(self.temperature_k(), 1.0))
            lwc = qc * rho * 1000.0               # g m-3
            # 云内：能见度随含水量迅速下降（Kunkel 1984 型经验式，量级正确）
            vis_cloud_km = np.clip(1.0 / np.maximum(lwc, 1e-3) * 0.3, 0.02, 30.0)
            vis_km = np.where(lwc > 0.01, np.minimum(vis_km, vis_cloud_km), vis_km)

        vis_m = vis_km * 1000.0
        with np.errstate(divide="ignore", invalid="ignore"):
            ext = np.where(vis_km > 1e-3, 3.912 / np.maximum(vis_km, 1e-3), 999.0)
        return vis_m, ext

    # ── 风险（沿用平台 v2 的口径）────────────────────────────────────────
    def risk_summary(self, tke: np.ndarray, vis_m: np.ndarray,
                     cape: np.ndarray, cin: np.ndarray) -> Dict[str, Any]:
        """按本项目 `algorithms/risk/weather_risk.py` 的口径汇总风险。

        口径：risk_score = 0.35·风 + 0.25·能见度 + 0.25·湍流 + 0.15·降水，
              各归一化到 [0,1]；risk_level ∈ low | medium | high | critical。
        ★ 用平台自己的约定，而不是 Java mock 的 LOW/MODERATE/HIGH/EXTREME。
        ★★ 旧版 docstring 写的是"0.4 风 + 0.3 能见度 + 0.3 降水"，与实现
           （0.35/0.25/0.25/0.15，含湍流项）**不符** —— 注释与代码不一致本身就是 bug。
        ★ 降水项**无法计算时置 None 并把权重按比例摊回其余三项**，
           绝不拿 0 冒充"已评估"。
        取**低空最不利值**（低空是无人机活动层，保守取向）。
        """
        ws = self.surface_wind_speed()
        ws_max = float(np.max(ws)) if ws is not None else 0.0
        tke_max = float(np.max(tke)) if tke.size else 0.0
        tke_mean = float(np.mean(tke)) if tke.size else 0.0
        vis_min = float(np.min(vis_m)) / 1000.0 if vis_m.size else 30.0   # km

        wind_thr = 15.0      # m/s
        vis_thr = 5.0        # km

        # ★FIX-RAIN：旧版写 `rain = float(np.max(self._get("RAINNC"))) * 0.0`
        #   ⇒ 降水因子**恒为 0**，却仍占 15% 权重。两个后果：
        #     ① risk_score 白白少掉 0.15 的动态范围；
        #     ② `factors.precipitation_risk = 0` 会被下游**误读成"确实没降水"**。
        #   RAINNC 是**自模式起报以来的累积量**，要得到雨强必须**对时间做差**。
        #   ⇒ 能算就算；算不出来就显式置 None 并把权重按比例分回其余三项，
        #     **绝不拿累积量冒充雨强、也不拿 0 假装"已评估"**（与本项目"禁止硬编码兜底"一致）。
        rain_mm = None
        if self.has("RAINNC"):
            # ★ 注意：`_get()` 会**把 Time 维压掉**（返回当前 self.ti 的那一帧），
            #   所以做差必须直接读原始变量，不能用 _get。
            raw = self.ds.variables["RAINNC"]
            ntime = raw.shape[0] if ("Time" in raw.dimensions and raw.ndim >= 1) else 0
            if ntime > self.ti + 1:
                d = (np.asarray(raw[self.ti + 1], dtype=np.float64)
                     - np.asarray(raw[self.ti], dtype=np.float64))
                rain_mm = float(np.max(d))
                self.notes.append(
                    "小时雨强 = RAINNC(t=%d→%d) 之差（累积量必须做差，不能直接用）"
                    % (self.ti, self.ti + 1))
            else:
                self.notes.append(
                    "RAINNC 存在但无法做差（ti=%d，Time 维长度=%s）"
                    "⇒ precipitation_risk 置 None，权重按比例分回其余三项"
                    % (self.ti, ntime or "?"))

        wind_risk = min(1.0, max(0.0, ws_max / wind_thr))
        vis_risk = min(1.0, max(0.0, 1.0 - vis_min / vis_thr))
        tke_risk = min(1.0, max(0.0, tke_max / 3.0))          # 3 m²/s² 视为严重
        precip_risk = (None if rain_mm is None
                       else min(1.0, max(0.0, rain_mm / 10.0)))

        # 有降水：0.35 风 / 0.25 能见度 / 0.25 湍流 / 0.15 降水
        # 无降水：把 0.15 按原比例摊回前三项（**不是**当 0 算）
        if precip_risk is None:
            _w = (0.35, 0.25, 0.25)
            _tot = sum(_w)
            score = (_w[0] * wind_risk + _w[1] * vis_risk + _w[2] * tke_risk) / _tot
        else:
            score = (0.35 * wind_risk + 0.25 * vis_risk
                     + 0.25 * tke_risk + 0.15 * precip_risk)
        level = ("low" if score < 0.3 else
                 "medium" if score < 0.6 else
                 "high" if score < 0.8 else "critical")

        return {
            "risk_score": _r(score, 4),
            "risk_level": level,
            "factors": {
                "wind_risk": _r(wind_risk, 4),
                "visibility_risk": _r(vis_risk, 4),
                "turbulence_risk": _r(tke_risk, 4),
                # None 表示**未评估**，与"评估了、结果是 0"必须区分开
                "precipitation_risk": (None if precip_risk is None
                                       else _r(precip_risk, 4)),
            },
            "thresholds": {"wind_speed_ms": wind_thr, "visibility_km": vis_thr,
                           "tke_m2s2": 3.0},
            "weights": ({"wind": 0.35, "visibility": 0.25, "turbulence": 0.25}
                        if precip_risk is None else
                        {"wind": 0.35, "visibility": 0.25, "turbulence": 0.25,
                         "precipitation": 0.15}),
            "worst": {
                "wind_speed_ms": _r(ws_max, 2),
                "visibility_km": _r(vis_min, 3),
                "tke_max_m2s2": _r(tke_max, 3),
                "tke_mean_m2s2": _r(tke_mean, 4),
                "rain_mm_h": (None if rain_mm is None else _r(rain_mm, 2)),
            },
        }


# ═════════════════════════════════════════════════════════════════════════════
#  4. auto_stride：防止 JSON 超过 Java 的 10 MB 上限
# ═════════════════════════════════════════════════════════════════════════════
def auto_stride(nlev: int, ny: int, nx: int, stride: int) -> int:
    """估算输出字节数，必要时加大 stride。返回实际使用的 stride。

    ★ 用真实的大数组个数 N_ARRAYS（不是 3）。首版低估 3 倍，
      导致 18.2 MB 的输出"以为"只有 5.3 MB。
    """
    s = max(1, int(stride))
    while s < 128:
        npts = nlev * math.ceil(ny / s) * math.ceil(nx / s)
        est = npts * N_ARRAYS * BYTES_PER_VALUE
        if est <= MAX_OUT_BYTES:
            return s
        s += 1
    return s


def _subsample(a: np.ndarray, stride: int) -> np.ndarray:
    if stride <= 1:
        return a
    return a[..., ::stride, ::stride]


def _bl_block(surf: Dict[str, np.ndarray]) -> Dict[str, Any]:
    """★FIX13：把 `PBLH` / `UST` / `HFX` 真正用起来。

    此前这三个量被 `surface()` 读进来之后**从未被任何计算使用** ——
    "读了不用"与"文件里有≠会被用上"是同一类问题，只是更隐蔽
    （它们出现在返回值里，看起来像是用上了）。

    三者对低空无人机都是**直接相关**的：
      · `PBLH` 边界层高度：BL 内湍流强、BL 以上多为自由大气，风廓线形态完全不同
      · `UST`  摩擦速度  ：地表动量通量尺度，夜间稳定边界层的关键参数
      · `HFX`  感热通量  ：对流触发与热力湍流的驱动项
    """
    out: Dict[str, Any] = {
        "method": "WRF 原生诊断量（PBLH/UST/HFX），非参数化",
        "available": [],
    }
    for k, unit, desc in (("PBLH", "m", "边界层高度"),
                          ("UST", "m s-1", "摩擦速度"),
                          ("HFX", "W m-2", "感热通量")):
        a = surf.get(k)
        if a is None:
            continue
        a = np.asarray(a, dtype=np.float64)
        out["available"].append(k)
        out[k.lower()] = {
            "min": _r(float(np.min(a)), 3),
            "mean": _r(float(np.mean(a)), 3),
            "max": _r(float(np.max(a)), 3),
            "unit": unit,
            "desc": desc,
        }
    if not out["available"]:
        out["note"] = "本次文件里没有 PBLH/UST/HFX（若 bl_pbl_physics=1 应当有）"
    return out


# ═════════════════════════════════════════════════════════════════════════════
#  5. 主处理：组装前端 payload
# ═════════════════════════════════════════════════════════════════════════════
def build_payload(file_path: str,
                  levels_m: Optional[List[float]] = None,
                  stride: int = 1,
                  want_risk: bool = True) -> Dict[str, Any]:
    """读 wrfout → 前端 `NetCDFPreview.vue` 直接可用的 payload。

    ★FIX5：输出完整 [level][lat][lon] 三维场，不再压成时间序列。
    """
    rd = WrfReader(file_path)
    if not rd.open():
        return {"success": False, "error": f"无法打开文件: {os.path.basename(file_path)}"}

    try:
        if not all(rd.has(v) for v in ("T", "P", "PB", "U", "V", "PH", "PHB")):
            missing = [v for v in ("T", "P", "PB", "U", "V", "PH", "PHB") if not rd.has(v)]
            return {"success": False,
                    "error": f"不是有效的 wrfout，缺少变量: {', '.join(missing)}"}

        targets = [float(x) for x in (levels_m or DEFAULT_LEVELS_M)]
        nlev = len(targets)
        surf = rd.surface()

        lat, lon = rd.latlon()
        if lat.size == 0:
            return {"success": False, "error": "缺少 XLAT/XLONG，无法建立坐标"}

        ny, nx = lat.shape
        st = auto_stride(nlev, ny, nx, stride)
        if st != stride:
            rd.notes.append(
                f"为保证输出 < {MAX_OUT_BYTES // 1024 // 1024} MB，"
                f"stride 由 {stride} 自动调整为 {st}（网格 {ny}×{nx} × {nlev} 层）")

        theta = rd.theta_k()
        T = rd.temperature_k()                     # ★FIX2
        U, V = rd.wind_uv()
        z = rd.height_agl()
        W = rd.wind_w()                            # ★FIX6（W 用于上升/下沉气流统计）
        W_raw = W                                  # 风险块直接引用原始 W（未插值、33 层）

        # 锚点：T/Q 用 2 m（T2/Q2），风用 10 m（U10/V10，WRF 无 2 m 风）
        T_l = rd.interp_to_levels(T, z, targets,
                                  anchor_val=surf.get("T2"), anchor_z=2.0)
        Q_l = rd.interp_to_levels(rd.qvapor(), z, targets,
                                  anchor_val=surf.get("Q2"), anchor_z=2.0)
        U_l = rd.interp_to_levels(U, z, targets,
                                  anchor_val=surf.get("U10"), anchor_z=10.0)
        V_l = rd.interp_to_levels(V, z, targets,
                                  anchor_val=surf.get("V10"), anchor_z=10.0)
        if W is not None:
            W_l = rd.interp_to_levels(W, z, targets)
        else:
            W_l = np.zeros_like(T_l)

        # 逐层的派生量（同一套垂直坐标）
        p_l = rd.interp_to_levels(rd.pressure_pa(), z, targets,
                                  anchor_val=surf.get("PSFC"), anchor_z=2.0)
        rh_l = np.clip(100.0 * Q_l / np.maximum(_qs(p_l, T_l), 1e-12), 0.0, 100.0)
        ws_l = np.sqrt(U_l ** 2 + V_l ** 2)

        # 风险量用**未抽样的原始场**
        tke, eps = rd.tke_mixing_length()
        ri = rd.richardson_number()
        vis_m, ext = rd.visibility_m()
        cape, cin, lcl, lfc = rd.parcel_cape_cin()

        # ★ 垂直坐标一致性：tke 原始在 33 个模式层上，而 temperature 在 12 个目标层上。
        #   若直接把 33 层的 tke 放进 payload，会出现"同一个 payload 里两个字段层数不同"，
        #   前端滑块按 dims.level=12 索引就会错位。⇒ 把 tke 也插值到同一套目标层。
        tke_l = rd.interp_to_levels(tke, z, targets)

        def _t(a):     # 抽样 + 转 list（3-D）
            return _arr2list(_subsample(np.asarray(a, dtype=np.float64), st), 2)

        def _t2(a):    # 抽样 + 转 list（2-D，用于列积分量如 CAPE）
            return _arr2list(_subsample(np.asarray(a, dtype=np.float64), st), 2)

        payload: Dict[str, Any] = {
            "fileName": os.path.basename(file_path),
            "fileSize": os.path.getsize(file_path) if os.path.exists(file_path) else 0,
            # ★FIX-DIMS：`dims` 必须描述**输出数组的形状**（即 payload 里
            #   temperature/u/v 的实际层数与行列数），否则 stride>1 时
            #   dims 报原始网格、数组却是抽样后的 ⇒ **契约不一致**。
            #   实测 stride=3、域 130×150 时，dims.lat 报 130 而 temperature 只有 44 行。
            #   （旧版侥幸没炸，只因前端 NetCDFPreview.vue 拿 dims 只拼 shape 做日志。）
            #   原始网格尺寸改放 `grid.sourceDims`，信息不丢。
            "dims": {"time": 1, "level": nlev,
                     "lat": len(lat[::st, 0]), "lon": len(lon[0, ::st])},
            "vars": ["T", "U", "V", "W", "RH", "WS"],
            "levelNames": [f"{int(h) if float(h).is_integer() else h}m AGL" for h in targets],
            "grid": {
                "lat": [_r(v, 4) for v in lat[::st, 0]],
                "lon": [_r(v, 4) for v in lon[0, ::st]],
                "levels": [float(h) for h in targets],
                "sourceDims": {"lat": int(lat.shape[0]), "lon": int(lon.shape[1]),
                               "stride": int(st)},
            },
            # ── 前端三个主字段（注意：温度单位 K，前端自己 −273.15）──
            "temperature": _t(T_l),
            "u": _t(U_l),
            "v": _t(V_l),
            # ── 扩展字段（前端可选用）──
            "rh": _t(rh_l),
            "w": _t(W_l),
            "wind_speed": _t(ws_l),
            "pressure_hpa": _t(p_l / 100.0),
            # ★ 把 tke / cape 放进 payload（而不只放 risk 的标量）：
            #   ① 前端可以直接画"湍流风险场/对流风险场"图层；
            #   ② 更重要的是**可测试** —— 自测需要拿到整场才能做
            #      "CAPE 应与地面温度正相关"这类**符号敏感**的检验，
            #      只给 cape_max 一个标量是查不出浮力积分符号写反的。
            #   ⚠️ 维度差异是刻意的：tke 是三维场（已插值到与 temperature 同一套层），
            #      **cape 是二维列积分量**（每列一个值），没有垂直维。
            "tke": _t(tke_l),
            "cape": _t2(cape),
        }

        result: Dict[str, Any] = {
            "success": True,
            "payload": payload,
            "meta": {
                "source": "wrf_processor.py v2",
                "filePath": file_path,
                "stride": st,
                "levels_m_agl": [float(h) for h in targets],
                "time": _read_time(rd),
                "physics": {
                    "theta_to_T": "Poisson: T = theta*(p/p0)^(Rd/cp), Rd=287.0 cp=1004.5",
                    "rh": "Bolton(1980) es; RH=100*q/qs",
                    "tke": "k-l equilibrium: TKE ~ l^2 S^2 (1-Ri/Ri_c), l=Blackadar",
                    "cape": "parcel ascent (dry -> LCL -> pseudoadiabat), Tv-based",
                    "visibility": "Stoelinga & Warner (1999) RH relation (PARAMETERIZED)",
                },
                # ★FIX12：位温基准的**自证证据**。下游据此判断这份温度场
                #   能不能直接用于结论 —— 而不是只看 success=true。
                "theta_base": getattr(rd, "theta_base_evidence", {}) or {},
                "notes": list(rd.notes),
                "warnings": [],
            },
        }

        # ★ 明确标注哪些是诊断量、哪些是参数化 —— 论文口径需要
        if not rd.has("QKE") and not rd.has("TKE"):
            result["meta"]["warnings"].append(
                "wrfout 无 QKE/TKE（bl_pbl_physics=1）⇒ 湍流为 k-l 参数化估计，"
                "**不是模式诊断量**")
        if not rd.has("CAPE"):
            result["meta"]["warnings"].append(
                "wrfout 无 CAPE/CIN 诊断输出 ⇒ 由地面气块抬升法计算（非模式诊断量）")
        result["meta"]["warnings"].append(
            "能见度为 RH 经验参数化（Stoelinga & Warner 1999），非第一性原理")

        if want_risk:
            result["risk"] = {
                "summary": rd.risk_summary(tke, vis_m, cape, cin),
                "turbulence": {
                    "tke_mean": _r(np.mean(tke), 4),
                    "tke_max": _r(np.max(tke), 4),
                    "dissipation_rate": _r(np.mean(eps), 4),
                    "unit": "m2 s-2",
                    "method": "k-l equilibrium (NOT a model diagnostic)",
                    # ★FIX6（更正后）：W 用在它**真正该用的地方** —— 上升/下沉气流。
                    #   不塞进 TKE：k-l 闭合的 TKE 由切变与层结决定，本来就不需要 w。
                    #   这两项才是"下沉/上升气流"的字面含义，也是无人机最关心的量之一。
                    "w_max_ms": (None if W_raw is None
                                 else _r(float(np.max(W_raw)), 3)),
                    "w_min_ms": (None if W_raw is None
                                 else _r(float(np.min(W_raw)), 3)),
                    "w_note": ("W 来自 wrfout 的垂直速度；仅用于上升/下沉气流统计，"
                               "**未**参与 TKE（TKE 用 k-l 平衡闭合）"),
                },
                "visibility": {
                    "visibility_min_m": _r(np.min(vis_m), 1),
                    "visibility_mean_m": _r(np.mean(vis_m), 1),
                    "extinction_coefficient_mean": _r(np.mean(ext), 3),
                    "method": "Stoelinga & Warner (1999), parameterized",
                },
                "convection": {
                    "cape_max": _r(np.max(cape), 1),
                    "cape_mean": _r(np.mean(cape), 1),
                    "cin_min": _r(np.min(cin), 1),
                    "lcl_mean_m": _r(np.mean(lcl), 1),
                    # ★FIX-LFC：`lfc` 里"本列从未找到 LFC"是 **0 m**（L546 的初值），
                    #   只有正浮力列会被覆盖（L596）。直接 `np.mean(lfc)` 会把这些
                    #   **假 0** 一起平均进去。已数值演示：三列只有一列真的找到 LFC(600 m) 时，
                    #   均值从 600 m 被拉到 **200 m（偏低 67%）**。
                    #   ⇒ 只对**真的找到 LFC 的列**取平均，并把列数一并报出来（下游可判断代表性）。
                    "lfc_mean_m": _r(float(np.mean(lfc[lfc > 0])) if np.any(lfc > 0) else 0.0, 1),
                    "lfc_valid_cols": int(np.count_nonzero(lfc > 0)),
                    "lfc_total_cols": int(lfc.size),
                    "method": "parcel ascent from T2/Q2/PSFC",
                },
                "richardson": {
                    "ri_min": _r(np.min(ri), 3),
                    "ri_mean": _r(np.mean(ri), 3),
                    "note": "Ri < 0.25 为湍流活跃层",
                },
                # ★FIX13（§5.2-7）：`PBLH` / `UST` / `HFX` 此前**被读进来了却从不使用** ——
                #   "读了不用"和"文件里有≠会被用上"是同一类问题，而且更隐蔽：
                #   变量出现在 `surface()` 的返回里，看起来像是用上了。
                #   ⇒ 现在把三者用起来，而且它们对无人机是**真正有用**的量：
                #     · `pblh`  边界层高度 —— 决定无人机在 BL 内还是自由大气（湍流强度差一个量级）
                #     · `ust`   摩擦速度   —— 地表动量通量尺度，夜间稳定边界层的关键参数
                #     · `hfx`   感热通量   —— 对流触发/热力湍流的驱动
                "boundary_layer": _bl_block(surf),
            }

        # ── ★ 真实尺寸门禁：估算可能不准，这里用**实测**再兜一次 ──
        #    Java 的 PythonScriptInvoker 超过 10 MB 时是 `break` **静默截断**，
        #    前端会拿到半个 JSON 且看不出原因。所以必须自己保证不超。
        try:
            real = len(json.dumps(result, ensure_ascii=False).encode("utf-8"))
        except (TypeError, ValueError):
            real = 0
        if real > MAX_OUT_BYTES and st < 128:
            logger.warning("实测输出 %.1f MB 超限，stride %d → %d 重算",
                           real / 1024 / 1024, st, st + 1)
            rd.notes.append(f"实测 {real / 1024 / 1024:.1f} MB 超限，"
                            f"stride 自动加大到 {st + 1}")
            return build_payload(file_path, levels_m=levels_m,
                                 stride=st + 1, want_risk=want_risk)

        result["meta"]["outputBytes"] = real
        return result

    finally:
        rd.close()


def _read_time(rd: WrfReader) -> Optional[str]:
    try:
        if rd.has("Times"):
            t = rd.ds.variables["Times"][:]
            return str(nc.chartostring(t[0])) if np.ndim(t) > 1 else str(t[0])
    except Exception:                                 # noqa: BLE001
        pass
    return None


# ═════════════════════════════════════════════════════════════════════════════
#  6. 可选：接入真实 Pa 集合方差场
# ═════════════════════════════════════════════════════════════════════════════
def attach_pa_variance(result: Dict[str, Any], pa_path: str,
                       stride: int = 1) -> Dict[str, Any]:
    """把本项目 40 成员 Pa 双扰动集合的**真实**离散度场并入结果。

    ★ 为什么不用平台 v2 的 `AdaptiveVarianceField`：
      它的 `flow_dependent_variance = np.var(xb + randn*σ, axis=0)` **恒等于 σ²**，
      与背景场无关 —— 名字叫流依赖，实际是结构性常数。
      我们要的是真集合（WRFDA 40 成员）算出的空间变化离散度。

    期望文件变量（由 `export_pa_variance.py` 产出）：
        XLAT(ny,nx), XLONG(ny,nx),
        SPREAD_U(nz,ny,nx), SPREAD_V, SPREAD_T, SPREAD_QV
    """
    try:
        ds = nc.Dataset(pa_path, "r")
    except Exception as e:                            # noqa: BLE001
        result.setdefault("meta", {}).setdefault("warnings", []).append(
            f"Pa 方差场读取失败（{e}），已跳过")
        return result

    try:
        fields: Dict[str, Any] = {}
        for v in ("SPREAD_U", "SPREAD_V", "SPREAD_T", "SPREAD_QV"):
            if v in ds.variables:
                a = np.asarray(ds.variables[v][:], dtype=np.float64)
                if a.ndim == 3:
                    a = a[0] if a.shape[0] == 1 else a
                fields[v.replace("SPREAD_", "")] = _arr2list(_subsample(a, stride), 4)

        grid = {}
        for v, k in (("XLAT", "lat"), ("XLONG", "lon")):
            if v in ds.variables:
                a = np.asarray(ds.variables[v][:], dtype=np.float64)
                if a.ndim == 3:
                    a = a[0]
                grid[k] = [_r(x, 4) for x in a[::stride, 0] if k == "lat"] or \
                          [_r(x, 4) for x in a[0, ::stride]]

        result["pa_variance"] = {
            "available": bool(fields),
            "source": os.path.basename(pa_path),
            "members": int(getattr(ds, "n_members", 0)) or None,
            "note": ("40 成员双扰动集合的 1σ 离散度（WRFDA RANDOMCV 生成），"
                     "可作为高风险区判据/置信度权重 —— 这是**真实集合**统计量，"
                     "不是经验公式。"),
            "fields": fields,
            "grid": grid,
        }
        if not fields:
            result.setdefault("meta", {}).setdefault("warnings", []).append(
                "Pa 方差文件里没有 SPREAD_* 变量")
    finally:
        ds.close()
    return result


# ═════════════════════════════════════════════════════════════════════════════
#  7. action 分发
# ═════════════════════════════════════════════════════════════════════════════
def action_parse(params: Dict[str, Any]) -> Dict[str, Any]:
    fp = params.get("filePath") or params.get("file_path") or params.get("path")
    if not fp:
        return {"success": False, "error": "缺少 filePath 参数"}
    if not os.path.exists(fp):
        return {"success": False, "error": f"文件不存在: {fp}"}

    # ★FIX-HEIGHT：Java 的 `WrfController` 只传 `height`（(单个或一组) m AGL），
    #   而本脚本原来**只认 `levels`** ⇒ **调用方指定的高度被静默忽略**，
    #   一律走 DEFAULT_LEVELS_M 的 12 层。调用方以为传了、脚本以为没传，
    #   **没有任何一方报错** —— 正是本项目反复记录的那类"看着通其实没通"。
    #   现在 levels / height / heightMeters 三种写法都认（levels 优先）。
    levels = params.get("levels")
    if not levels:
        h = params.get("height", params.get("heightMeters"))
        if h is not None:
            try:
                levels = [float(x) for x in (h if isinstance(h, (list, tuple)) else [h])]
            except (TypeError, ValueError):
                return {"success": False,
                        "error": "height 参数无法解析为数值: %r" % (h,)}
    stride = int(params.get("stride", 1) or 1)
    res = build_payload(fp, levels_m=levels, stride=stride, want_risk=True)

    if res.get("success"):
        res.setdefault("meta", {})["requestedLevels"] = (
            list(levels) if levels else "默认 12 层（请求里没给 levels/height）")

    if res.get("success") and params.get("paVariancePath"):
        res = attach_pa_variance(res, params["paVariancePath"], stride=stride)
    return res


def action_variance(params: Dict[str, Any]) -> Dict[str, Any]:
    """只返回风险/方差相关字段（轻量查询）。"""
    fp = params.get("filePath")
    if not fp or not os.path.exists(fp):
        return {"success": False, "error": f"文件不存在: {fp}"}
    rd = WrfReader(fp)
    if not rd.open():
        return {"success": False, "error": "无法打开文件"}
    try:
        tke, eps = rd.tke_mixing_length()
        vis_m, ext = rd.visibility_m()
        cape, cin, lcl, lfc = rd.parcel_cape_cin()
        out = {
            "success": True,
            "risk": {"summary": rd.risk_summary(tke, vis_m, cape, cin)},
            "turbulence": {"tke_mean": _r(np.mean(tke), 4), "tke_max": _r(np.max(tke), 4)},
            "visibility": {"min_m": _r(np.min(vis_m), 1)},
            "convection": {"cape_max": _r(np.max(cape), 1), "cin_min": _r(np.min(cin), 1)},
        }
        if params.get("paVariancePath"):
            out = attach_pa_variance(out, params["paVariancePath"])
        return out
    finally:
        rd.close()


def action_validate(params: Dict[str, Any]) -> Dict[str, Any]:
    """检查文件是不是**可用的 wrfout**，并报告缺哪些量。"""
    fp = params.get("filePath")
    if not fp or not os.path.exists(fp):
        return {"success": False, "error": f"文件不存在: {fp}"}
    rd = WrfReader(fp)
    if not rd.open():
        return {"success": False, "error": "无法打开文件"}
    try:
        required = ["T", "P", "PB", "U", "V", "PH", "PHB", "XLAT", "XLONG"]
        optional = ["QVAPOR", "Q2", "T2", "PSFC", "U10", "V10", "HGT",
                    "W", "QCLOUD", "PBLH", "UST", "HFX",
                    "QKE", "TKE", "CAPE", "CIN", "LCL"]
        miss_req = [v for v in required if not rd.has(v)]
        return {
            "success": len(miss_req) == 0,
            "isWrfout": len(miss_req) == 0,
            "missing_required": miss_req,
            "present_optional": [v for v in optional if rd.has(v)],
            "absent_optional": [v for v in optional if not rd.has(v)],
            "dims": {k: len(v) for k, v in rd.ds.dimensions.items()},
            "time": _read_time(rd),
            "note": ("QKE/TKE/CAPE/CIN/LCL 缺席时，湍流与对流参数由本脚本"
                     "按物理正解计算，并在 meta.warnings 中标注为参数化/非诊断量"),
        }
    finally:
        rd.close()


def action_variables(params: Dict[str, Any]) -> Dict[str, Any]:
    fp = params.get("filePath")
    if not fp or not os.path.exists(fp):
        return {"success": False, "error": f"文件不存在: {fp}"}
    rd = WrfReader(fp)
    if not rd.open():
        return {"success": False, "error": "无法打开文件"}
    try:
        out = []
        for name, var in rd.ds.variables.items():
            out.append({
                "name": name,
                "shape": list(var.shape),
                "dims": list(var.dimensions),
                "units": getattr(var, "units", ""),
                "description": getattr(var, "description", "")[:80],
            })
        return {"success": True, "count": len(out), "variables": out}
    finally:
        rd.close()


ACTIONS = {
    "parse": action_parse,
    "compute": action_parse,
    "execute": action_parse,
    "transform": action_parse,
    "variance": action_variance,
    "validate": action_validate,
    "variables": action_variables,
}


# ═════════════════════════════════════════════════════════════════════════════
#  8. 入口（★ 与 PythonScriptInvoker 的契约一致）
# ═════════════════════════════════════════════════════════════════════════════
def _force_utf8_stdio() -> None:
    """★ 2026-09-28：把 stdout/stderr 钉成 UTF-8（本脚本自己保证，不依赖调用方环境）。

    背景（两次实测，不是推断）：
      · Windows 上子进程 stdout 默认是 **GBK**，而本脚本用 ``ensure_ascii=False``
        输出 JSON ⇒ 结果里只要出现一个 GBK 编不出的字符（实测 U+2212 减号）就会
        ``UnicodeEncodeError: 'gbk' codec can't encode character``，
        而且是在**已经写出 1.1 MB 之后**崩掉，留下半段 JSON + Traceback。
      · 同目录的 ``validate_physics.py`` / ``selftest_synthetic.py`` 也踩了同一个坑
        （不加 ``PYTHONIOENCODING=utf-8`` 直接崩）——说明"靠环境"不可靠。

    Java 侧（``PythonScriptInvoker``）已在子进程环境里设了
    ``PYTHONIOENCODING=utf-8`` + ``PYTHONUTF8=1``；这里再兜一层，
    保证**手工跑、被别的脚本调用、或在别的容器里跑**时输出同样是 UTF-8。
    """
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8")       # Python 3.7+
        except Exception:                              # noqa: BLE001
            pass


def main(argv: Optional[List[str]] = None) -> int:
    _force_utf8_stdio()
    argv = list(sys.argv[1:] if argv is None else argv)

    if len(argv) < 2:
        print(json.dumps({
            "success": False,
            "error": "用法: wrf_processor.py <action> <params_json_file>",
            "allowedActions": sorted(ALLOWED_ACTIONS),
        }, ensure_ascii=False))
        return 1

    action = argv[0]
    params_file = argv[1]

    if action not in ACTIONS:
        print(json.dumps({
            "success": False,
            "error": f"不支持的 action: {action}",
            "allowed": sorted(ACTIONS.keys()),
        }, ensure_ascii=False))
        return 1

    try:
        with open(params_file, "r", encoding="utf-8") as f:
            params = json.load(f)
    except Exception as e:                            # noqa: BLE001
        print(json.dumps({"success": False,
                          "error": f"参数文件读取失败: {e}"}, ensure_ascii=False))
        return 1

    if not isinstance(params, dict):
        print(json.dumps({"success": False, "error": "参数必须是 JSON 对象"},
                         ensure_ascii=False))
        return 1

    try:
        result = ACTIONS[action](params)
    except Exception as e:                            # noqa: BLE001
        logger.exception("处理失败")
        result = {"success": False, "error": f"{type(e).__name__}: {e}"}

    text = json.dumps(result, ensure_ascii=False, allow_nan=False)
    size = len(text.encode("utf-8"))
    if size > MAX_OUT_BYTES:
        logger.warning("输出 %d 字节，超过自设上限 %d", size, MAX_OUT_BYTES)
        result.setdefault("meta", {}).setdefault("warnings", []).append(
            f"输出 {size} 字节，接近 Java 的 10 MB 上限；建议加大 stride")
        text = json.dumps(result, ensure_ascii=False, allow_nan=False)

    print(text)
    logger.info("完成 action=%s 输出=%d 字节", action, size)
    return 0


if __name__ == "__main__":
    sys.exit(main())
