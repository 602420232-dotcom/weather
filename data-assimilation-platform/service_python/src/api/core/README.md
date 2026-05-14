# core

Python 微服务的核心业务逻辑层，封装同化计算的执行引擎，协调 Dask 集群与同化算法的交互。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：`AssimilationService` |
| `assimilation_service.py` | `AssimilationService`，同化计算核心服务：管理同化任务执行、集群交互、结果处理 |
| `test_assimilation_service.py` | 核心服务单元测试 |

## 使用示例

```python
from api.core.assimilation_service import AssimilationService

# 创建服务实例
service = AssimilationService(cluster_manager)

# 提交同化任务
job_id = service.submit_assimilation(
    background=background_data,
    observations=obs_data,
    method="3dvar"
)

# 查询任务状态
status = service.get_job_status(job_id)

# 等待结果
result = service.wait_for_result(job_id, timeout=300)
```

## 说明

- `AssimilationService` 是 FastAPI 主应用与算法核心之间的桥梁
- 管理 Dask 计算任务的提交、监控和结果回收
- 支持任务队列，避免过载

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
