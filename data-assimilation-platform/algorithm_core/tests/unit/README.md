# unit

单元测试目录，对贝叶斯数据同化系统中各独立模块进行隔离测试，确保每个组件的正确性。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 包初始化 |
| `test_assimilator.py` | 同化器核心功能单元测试 |
| `test_config.py` | 配置模块单元测试 |
| `test_logging.py` | 日志模块单元测试 |
| `test_metrics.py` | 性能指标模块单元测试 |
| `test_validation.py` | 数据验证模块单元测试 |

## 测试覆盖

- **同化器**：初始化、网格设置、3D-VAR 计算、结果验证
- **配置**：加载、解析、环境切换、参数验证
- **日志**：日志级别、格式、文件输出
- **指标**：性能统计、数据质量评估、同化效果度量
- **验证**：数据格式校验、网格一致性、观测合理性

## 运行方式

```bash
cd algorithm_core

# 运行单元测试
pytest tests/unit/ -v

# 仅单元测试（自动标记）
pytest -v -m unit

# 指定文件
pytest tests/unit/test_assimilator.py -v
```

## 注意事项

- 单元测试应保持独立性，不依赖外部服务或网络
- 使用 `conftest.py` 中的 fixture 提供测试数据
- 测试会自动获得 `@pytest.mark.unit` 标记

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
