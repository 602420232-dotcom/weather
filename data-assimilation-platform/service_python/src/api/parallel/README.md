# parallel

Python 微服务的并行计算模块，管理 Dask 分布式计算集群的生命周期（启动、监控、关闭）。

## 主要文件

| 文件 | 说明 |
|------|------|
| `dask.py` | `DaskClusterManager`，Dask 集群管理器：启动 Worker、监控集群状态、任务调度 |
| `test_dask.py` | 集群管理器测试 |

## 使用示例

```python
from api.parallel.dask import DaskClusterManager

# 创建集群管理器
manager = DaskClusterManager(n_workers=4, threads_per_worker=2)

# 启动集群
await manager.start()

# 检查状态
status = manager.status()

# 提交计算任务
future = manager.submit(compute_function, *args)
result = await future

# 关闭集群
await manager.stop()
```

## 生命周期

在 `main.py` 中通过 FastAPI lifespan 管理：

```python
@asynccontextmanager
async def lifespan(app: FastAPI):
    cluster_manager = DaskClusterManager(n_workers=4, threads_per_worker=2)
    await cluster_manager.start()
    yield
    await cluster_manager.stop()
```

## 与算法核心的关系

本 `parallel/` 专注于微服务层的集群管理，而 `algorithm_core` 中的 `parallel/` 负责算法层面的并行计算实现。两者形成分层架构：
- **服务层**（本目录）：集群生命周期、任务调度
- **算法层**（algorithm_core）：同化计算的并行执行逻辑

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
