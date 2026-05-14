# utils

贝叶斯同化系统的工具模块，提供配置管理、日志、性能度量、数据验证和性能分析等通用功能支持。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：全部工具类与函数 |
| `config.py` | 配置管理：`BaseConfig`、`OptimizedConfig`、`AdaptiveConfig`、`CompatibleConfig`、`ConfigFactory` 等配置类 |
| `log_utils.py` | 日志工具：`setup_logging()` 统一日志初始化 |
| `metrics.py` | 性能指标：`PerformanceMetrics`（运行效率）、`DataQualityMetrics`（数据质量）、`AssimilationMetrics`（同化效果） |
| `validation.py` | 数据验证：`DataValidator` 数据合理性校验 |
| `profiler.py` | 性能分析：运行时间和内存分析工具 |
| `import_utils.py` | 导入工具：模块动态导入与依赖检查 |
| `test_config.py` | 配置测试 |
| `test_log_utils.py` | 日志测试 |
| `test_metrics.py` | 指标测试 |
| `test_validation.py` | 验证测试 |
| `test_profiler.py` | 性能分析测试 |
| `test_import_utils.py` | 导入工具测试 |

## 配置管理

```python
from bayesian_assimilation.utils.config import ConfigFactory

config = ConfigFactory.load("production")
method = config.method               # 同化方法
resolution = config.grid_resolution   # 网格分辨率
```

## 日志初始化

```python
from bayesian_assimilation.utils import setup_logging

logger = setup_logging(level="INFO", log_file="assimilation.log")
```

## 性能监控

```python
from bayesian_assimilation.utils.metrics import PerformanceMetrics, DataQualityMetrics

perf = PerformanceMetrics()
perf.start()
# ... 同化计算 ...
perf.stop()
print(perf.summary())

quality = DataQualityMetrics()
score = quality.evaluate(analysis_data)
```

## 数据验证

```python
from bayesian_assimilation.utils.validation import DataValidator

validator = DataValidator()
validator.validate_grid_consistency(observations, grid)
validator.validate_range(data, min_val=0.0, max_val=100.0)
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
