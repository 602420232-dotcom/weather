# tests

算法核心的测试套件，包含单元测试与集成测试。使用 pytest 框架，提供共享 fixture 和测试数据生成器。

## 主要文件

| 文件 | 说明 |
|------|------|
| `conftest.py` | pytest 全局配置与共享 fixture（测试配置、背景场、观测数据、风场、时间序列等） |
| `integration/__init__.py` | 集成测试包初始化 |
| `unit/__init__.py` | 单元测试包初始化 |

## 共享 Fixture

`conftest.py` 提供以下可复用 fixture：

| Fixture | 说明 |
|---------|------|
| `sample_config` | AssimilationConfig 测试配置 |
| `simple_config` | BaseConfig 简单配置 |
| `background_field` | 20×20×5 模拟背景场数据 |
| `observation_data` | 10 个随机观测点（含位置和误差） |
| `assimilation_result` | 完整同化结果（分析场 + 方差场） |
| `temp_output_dir` | 临时输出目录 |
| `wind_field` | 15×15×5 U/V 风分量数据 |
| `time_series_data` | 10 时间步 × 20 点的时间序列数据 |

## 运行测试

```bash
cd algorithm_core

# 运行所有测试
pytest tests/ -v

# 单元测试
pytest tests/unit/ -v

# 集成测试
pytest tests/integration/ -v

# 带覆盖率报告
pytest tests/ --cov=bayesian_assimilation --cov-report=html

# 跳过慢速测试
pytest tests/ -v -m "not slow"

# 运行所有 benchmark 测试
pytest benchmarks/ tests/ -v -m "benchmark or unit or integration"
```

## 测试标记

| 标记 | 说明 |
|------|------|
| `slow` | 慢速测试，可用 `-m "not slow"` 跳过 |
| `integration` | 集成测试，根据文件路径自动标记 |
| `unit` | 单元测试，根据文件路径自动标记 |
| `benchmark` | 性能基准测试 |

## 测试目录

| 目录 | 说明 |
|------|------|
| `unit/` | 单元测试：同化器、配置、日志、指标、验证 |
| `integration/` | 集成测试：完整同化工作流 |

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
