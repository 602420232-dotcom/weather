# components

贝叶斯同化系统的通用组件库，封装了可复用的计算和优化组件（GPU 加速、自适应分辨率、块分解、增量检测、稀疏协方差等）。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：全部组件类 |
| `gpu.py` | `GPUAccelerator`，GPU 运算加速组件 |
| `resolution.py` | `AdaptiveResolutionSelector`，根据计算资源和数据特征自适应选择网格分辨率 |
| `block_decomp.py` | `BlockDecomposition`，网格分块分解，支持子域独立计算 |
| `incremental.py` | `IncrementalDetector`，增量更新检测器，判断是否需要重新同化 |
| `covariance.py` | `FastSparseBackgroundCovariance`，快速稀疏背景协方差矩阵构建 |

## 使用示例

```python
from bayesian_assimilation.components import (
    GPUAccelerator,
    AdaptiveResolutionSelector,
    BlockDecomposition,
    FastSparseBackgroundCovariance,
)

# GPU 加速
gpu = GPUAccelerator()
gpu.initialize()
accelerated_data = gpu.transfer(data)

# 自适应分辨率
selector = AdaptiveResolutionSelector()
optimal_res = selector.select(domain_size, available_memory)

# 块分解
blocks = BlockDecomposition.decompose(grid, block_size=(25, 25, 25))

# 稀疏协方差
cov = FastSparseBackgroundCovariance(background, correlation_length=50.0)
sparse_matrix = cov.build()
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
