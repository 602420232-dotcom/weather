# models

贝叶斯同化算法的模型层实现，提供多种同化算法模型（3D-VAR、4D-VAR、EnKF、混合模型等），每个模型封装了特定算法的完整计算逻辑。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：全部算法模型类 |
| `base.py` | 模型抽象基类，定义算法模型的通用接口 |
| `three_dimensional_var.py` | `ThreeDimensionalVAR`，三维变分同化（空间域同化） |
| `four_dimensional_var.py` | `FourDimensionalVar`，四维变分同化（时空域同化，含时间窗口） |
| `enkf.py` | `EnKF`，集合卡尔曼滤波（基于集合的统计估计） |
| `hybrid.py` | `HybridAssimilation` / `AdaptiveHybridAssimilation`，多算法自适应混合同化 |
| `enhanced_bayesian.py` | `EnhancedBayesianAssimilation`，增强型贝叶斯同化 |
| `adaptive_assimilator.py` | 自适应同化器，根据数据特征自动调整参数 |
| `variance_field_optimizer.py` | `VarianceFieldOptimizer` / `AdaptiveVarianceField`，方差场优化 |
| `test_base.py` | 模型基类测试 |
| `test_adaptive_assimilator.py` | 自适应同化器测试 |

## 算法对比

| 算法 | 维度 | 原理 | 适用场景 |
|------|------|------|---------|
| **3D-VAR** | 3D 空间 | 最小化背景与观测的加权代价函数 | 单时刻静态场同化 |
| **4D-VAR** | 4D 时空 | 在时间窗口内寻找最优初始场 | 数值天气预报、动态系统 |
| **EnKF** | 3D/4D | 集合统计估计背景误差协方差 | 非线性系统、实时同化 |
| **Hybrid** | — | 自适应组合多种算法优势 | 复杂异构观测场景 |

## 使用示例

```python
from bayesian_assimilation.models import (
    ThreeDimensionalVAR, FourDimensionalVar, EnKF, HybridAssimilation
)

# 3D-VAR
model_3d = ThreeDimensionalVAR()
analysis, variance = model_3d.assimilate(background, obs, obs_loc)

# 4D-VAR
model_4d = FourDimensionalVar(time_window=6)
analysis, variance = model_4d.assimilate(background, obs_seq, obs_loc_seq)

# EnKF
model_enkf = EnKF(ensemble_size=50)
analysis, ensemble = model_enkf.assimilate(background, obs, obs_loc)

# 混合同化
model_hybrid = HybridAssimilation(methods=["3dvar", "enkf"])
analysis, variance = model_hybrid.assimilate(background, obs, obs_loc)
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
