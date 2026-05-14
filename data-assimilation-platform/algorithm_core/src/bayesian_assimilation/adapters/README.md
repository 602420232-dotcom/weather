# adapters

数据适配器模块，封装各种外部数据格式与贝叶斯同化系统内部标准格式之间的转换逻辑，包括 WRF 气象模型数据、网格插值、I/O 读写和无人机（UAV）数据适配。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：全部适配器类与工具函数 |
| `data.py` | 数据格式适配：`WRFDataAdapter`（WRF 模型输出适配）、`ObservationAdapter`（观测数据适配）、格式转换与验证 |
| `grid.py` | 网格适配：`GridAdapter`（网格变换）、插值 `interpolate_data`、重采样 `resample_data`、点/网格互转 |
| `io.py` | I/O 适配：`NetCDFReader`（NetCDF 读取）、`HDF5Reader`（HDF5 读取）、`write_netcdf`、`write_hdf5` 写入 |
| `uav_adapter.py` | 无人机数据适配：`UAVDataAdapter`（将 UAV 遥测转为标准同化格式）、`process_uav_data` |
| `test_data.py` | 数据适配器测试 |
| `test_grid.py` | 网格适配器测试 |
| `test_io.py` | I/O 适配器测试 |
| `test_uav_adapter.py` | UAV 适配器测试 |

## 数据格式适配

```python
from bayesian_assimilation.adapters import (
    WRFDataAdapter, convert_to_assimilation_format
)

# WRF 数据适配
wrf = WRFDataAdapter()
data = wrf.load("wrfout_d01.nc")
standard_format = convert_to_assimilation_format(data)

# 观测数据适配
obs = ObservationAdapter()
observations = obs.convert(raw_sensor_data)
```

## 网格适配

```python
from bayesian_assimilation.adapters import (
    interpolate_data, resample_data
)

# 插值到目标网格
interpolated = interpolate_data(data, source_grid, target_grid)

# 网格重采样
resampled = resample_data(data, scale_factor=2)
```

## I/O 适配

```python
from bayesian_assimilation.adapters import NetCDFReader, write_netcdf

reader = NetCDFReader("data.nc")
variables = reader.read()
write_netcdf("output.nc", analysis_data)
```

## UAV 适配

```python
from bayesian_assimilation.adapters import UAVDataAdapter, uav_to_standard_format

adapter = UAVDataAdapter()
standard_data = adapter.convert(uav_telemetry)
# 或直接使用
standard_data = uav_to_standard_format(uav_data)
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
