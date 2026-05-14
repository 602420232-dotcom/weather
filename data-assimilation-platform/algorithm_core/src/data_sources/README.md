# data_sources

多源气象数据接入模块，提供统一的数据源抽象和工厂模式，支持卫星、雷达、地面站、浮标等多种数据源类型的即插即用管理。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：DataSourceBase、各数据源类、工厂 |
| `base.py` | 数据源抽象基类 `DataSourceBase`，定义 `load_data()` 等标准接口 |
| `factory.py` | 数据源工厂 `DataSourceFactory`，支持注册、创建和查询数据源类型 |
| `satellite.py` | 卫星数据源 `SatelliteDataSource` |
| `radar.py` | 雷达数据源 `RadarDataSource` |
| `ground_station.py` | 地面站数据源 `GroundStationDataSource` |
| `buoy.py` | 浮标数据源 `BuoyDataSource` |
| `test_base.py` | 基类单元测试 |
| `test_factory.py` | 工厂模式单元测试 |
| `test_satellite.py` | 卫星数据源单元测试 |
| `test_radar.py` | 雷达数据源单元测试 |
| `test_ground_station.py` | 地面站数据源单元测试 |
| `test_buoy.py` | 浮标数据源单元测试 |

## 架构设计

```
DataSourceBase (抽象基类)
    ├── SatelliteDataSource      # 卫星遥感数据
    ├── RadarDataSource           # 雷达反射率数据
    ├── GroundStationDataSource   # 地面气象站观测
    └── BuoyDataSource            # 海洋浮标观测

DataSourceFactory (工厂模式)
    - create_data_source(type, config) -> DataSourceBase
    - register_data_source(type, class)
    - get_supported_types() -> list
```

## 使用示例

```python
from data_sources import DataSourceFactory

# 创建卫星数据源
satellite = DataSourceFactory.create_data_source("satellite", config={"band": "IR"})
satellite.load_data("path/to/data.nc")

# 查看所有支持的数据源
print(DataSourceFactory.get_supported_types())
# ['satellite', 'radar']

# 扩展自定义数据源
DataSourceFactory.register_data_source("custom", CustomDataSource)
```

## 扩展指南

新增数据源需要：
1. 继承 `DataSourceBase` 并实现 `load_data()` 方法
2. 通过 `DataSourceFactory.register_data_source()` 注册

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
