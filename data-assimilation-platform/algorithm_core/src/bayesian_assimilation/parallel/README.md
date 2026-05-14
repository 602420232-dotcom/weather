# parallel

贝叶斯同化系统并行计算框架，支持多种并行后端（区域分解、Dask、MPI、Ray），通过统一的工厂模式管理并行同化器实例。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：并行管理器、工厂、可用性检测 |
| `base.py` | `ParallelManager` 基类、`ParallelType` 枚举、`parallel_factory` 工厂 |
| `block.py` | `BlockParallelAssimilator`，区域分解并行（按地理子域划分） |
| `dask.py` | `DaskParallelManager`，Dask 分布式计算后端 |
| `mpi.py` | `MPIParallelManager`，MPI 多机多进程并行（可选依赖） |
| `ray.py` | `RayParallelManager`，Ray 分布式计算后端（可选依赖） |
| `test_base.py` | 基类测试 |
| `test_block.py` | 区域分解并行测试 |
| `test_dask.py` | Dask 并行测试 |
| `test_mpi.py` | MPI 并行测试 |
| `test_ray.py` | Ray 并行测试 |

## 并行后端对比

| 后端 | 适用场景 | 通信模式 | 依赖 |
|------|---------|---------|------|
| **Block** | 单机多核，网格可分 | 无通信（各块独立） | 内置 |
| **Dask** | 单机/小集群分布式 | 惰性计算图 | `dask` |
| **MPI** | HPC 多节点大规模并行 | 消息传递 (MPI) | `mpi4py`（可选） |
| **Ray** | 云原生分布式计算 | Actor/Task 模型 | `ray`（可选） |

## 使用示例

```python
from bayesian_assimilation.parallel import (
    ParallelType, parallel_factory, create_dask_client
)

# 通过工厂创建并行管理器
manager = parallel_factory.create(ParallelType.DASK, config={"n_workers": 4})

# 或直接创建
manager = create_dask_client(n_workers=4)

# 执行并行同化
result = manager.assimilate(background, observations, obs_locations)
```

## 可用性检测

```python
from bayesian_assimilation.parallel import MPI_AVAILABLE, RAY_AVAILABLE

print(f"MPI 可用: {MPI_AVAILABLE}")
print(f"Ray 可用: {RAY_AVAILABLE}")
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
