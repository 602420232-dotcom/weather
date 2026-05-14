# core

贝叶斯同化系统的核心算法模块，提供同化器的抽象基类、实现类以及支撑运算组件（协方差、求解器、统计量、算子等），是整个同化系统的计算引擎。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 包初始化（避免循环导入，按需加载） |
| `base.py` | 同化器抽象基类 `AssimilationBase`，定义标准同化接口 |
| `assimilator.py` | 主同化器 `BayesianAssimilator`，3D-VAR 核心实现 |
| `assimilator_advanced.py` | 高级同化器扩展，支持更多参数和诊断信息 |
| `compatible_assimilator.py` | `CompatibleAssimilator`，兼容旧版 API 的同化器实现 |
| `context.py` | 同化上下文管理 `AssimilationContext`，状态与参数容器 |
| `covariance.py` | 协方差矩阵运算，背景误差协方差构建与处理 |
| `operators.py` | 观测算子（H）、线性化算子等数值运算 |
| `solvers.py` | 线性/非线性求解器，用于变分同化的代价函数最小化 |
| `statistics.py` | 统计量计算：均值、方差、相关性、创新向量等 |
| `strategy.py` | 同化策略模式，支持不同算法的策略选择和组合 |
| `test_assimilator.py` | 同化器单元测试 |
| `test_base.py` | 基类单元测试 |
| `test_compatible_assimilator.py` | 兼容同化器测试 |
| `test_context.py` | 上下文管理测试 |
| `test_covariance.py` | 协方差测试 |
| `test_operators.py` | 算子测试 |
| `test_solvers.py` | 求解器测试 |
| `test_statistics.py` | 统计量测试 |
| `test_strategy.py` | 策略模式测试 |

## 核心架构

```
AssimilationBase (抽象基类)
    ├── BayesianAssimilator        # 标准 3D-VAR 同化器
    ├── CompatibleAssimilator       # 向后兼容的同化器
    └── AssimilationContext         # 同化状态容器

支撑组件：
    - CovarianceOperations          # 协方差矩阵构建
    - ObservationOperators          # 观测算子 H 及其伴随
    - LinearSolvers                  # 共轭梯度等求解器
    - AssimilationStatistics         # 同化统计与诊断
    - AssimilationStrategy           # 策略模式选择
```

## 使用示例

```python
from bayesian_assimilation.core.assimilator import BayesianAssimilator

assimilator = BayesianAssimilator()
assimilator.initialize_grid(domain_size=(100, 100, 50))

analysis, variance = assimilator.assimilate_3dvar(
    background=background_array,
    observations=obs_array,
    obs_locations=obs_locs
)
```

## 求解器说明

| 求解器 | 适用算法 | 说明 |
|--------|---------|------|
| 共轭梯度法 | 3D-VAR | 对称正定系统的标准求解 |
| 拟牛顿法 | 4D-VAR | 大规模非线性优化的高效选择 |
| SVD 分解 | EnKF | 集合协方差的正交分解 |

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
