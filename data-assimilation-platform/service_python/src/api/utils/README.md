# utils

Python 微服务的工具模块，提供数据序列化、格式验证等辅助功能。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：序列化与验证函数 |
| `serializers.py` | 数据序列化：`serialize_grid()`（网格序列化）、`serialize_analysis()`（分析场序列化） |
| `validators.py` | 数据验证：`validate_grid()`（网格一致性校验）、`validate_observations()`（观测数据校验） |
| `test_serializers.py` | 序列化测试 |
| `test_validators.py` | 验证器测试 |

## 序列化

```python
from api.utils import serialize_grid, serialize_analysis

# 网格数据序列化（为传输或存储准备）
encoded = serialize_grid(grid_data)

# 分析结果序列化
payload = serialize_analysis(analysis_field, variance_field)
```

## 验证

```python
from api.utils import validate_grid, validate_observations

# 校验网格一致性
validate_grid(grid_definition, background_data)

# 校验观测数据
validate_observations(observations, expected_variables)
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
