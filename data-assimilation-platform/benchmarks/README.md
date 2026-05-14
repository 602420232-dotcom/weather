# benchmarks

数据同化平台的整体性能基准测试套件，用于评估系统在不同场景下的端到端性能表现（吞吐量、延迟、资源消耗）。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 包初始化 |
| `conftest.py` | pytest 共享 fixture（测试数据和配置） |
| `test_3dvar_performance.py` | 3D-VAR 算法在多种网格规模下的性能测试 |
| `test_4dvar_performance.py` | 4D-VAR 算法在时间窗口扩展场景下的性能测试 |
| `test_enkf_scaling.py` | EnKF 集合卡尔曼滤波的集合规模扩展性测试 |

## 运行方式

```bash
cd data-assimilation-platform

# 运行所有基准测试
pytest benchmarks/ -v

# 仅基准测试标记
pytest benchmarks/ -v -m benchmark

# 指定算法测试
pytest benchmarks/test_3dvar_performance.py -v
```

## 测试矩阵

| 测试 | 网格规模 | 观测数范围 | 评估指标 |
|------|---------|-----------|---------|
| 3D-VAR | (50,50,20) → (200,200,100) | 500 → 5000 | 运行时间、收敛性 |
| 4D-VAR | 含时间窗口扩充 | — | 时间维度扩展效率 |
| EnKF | 集合成员数 10 → 100 | — | 集合规模扩展性 |

## 注意

- 与 `algorithm_core/benchmarks/` 不同，本目录位于平台根级别，侧重端到端的系统级性能评估
- 依赖 `algorithm_core` 包已安装

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
