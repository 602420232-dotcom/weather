# benchmarks

算法核心的性能基准测试套件，用于评估贝叶斯数据同化算法在不同网格规模下的运行时间、内存消耗和收敛速度。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 包初始化 |
| `conftest.py` | pytest 共享 fixture（提供 `small_grid` 等测试数据） |
| `test_3dvar_performance.py` | 3D-VAR 算法性能基准测试，参数化多网格规模 |
| `test_4dvar_performance.py` | 4D-VAR 算法性能基准测试 |
| `test_enkf_scaling.py` | EnKF 集合卡尔曼滤波扩展性基准测试 |

## 运行方式

```bash
cd algorithm_core

# 运行所有基准测试
pytest benchmarks/ -v

# 仅运行基准测试（跳过单元测试）
pytest benchmarks/ -v -m benchmark

# 指定算法
pytest benchmarks/test_3dvar_performance.py -v
```

## 测试参数

- **3D-VAR**：测试网格 `(50,50,20)` → `(200,200,100)`，观测点数 `500` → `5000`
- **4D-VAR**：包含时间维度扩展，评估多时间窗口性能
- **EnKF**：测试集合成员数从 `10` → `100` 的扩展性

## 配置说明

基准测试依赖 `conftest.py` 中定义的 fixture，包括 `small_grid` 等共享数据，测试标记为 `@pytest.mark.benchmark`。

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
