# services

Python 微服务的业务服务层，提供高层次的业务逻辑封装（如任务管理、调度等），供 API 路由层调用。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：`JobService` |
| `job_service.py` | `JobService`，作业管理服务：任务的创建、状态跟踪、结果查询 |
| `test_job_service.py` | 作业服务单元测试 |

## 使用示例

```python
from api.services import JobService

job_service = JobService()

# 创建任务
job = job_service.create_job(
    request=assimilation_request,
    priority="normal"
)

# 查询任务
status = job_service.get_job(job.job_id)

# 列出所有任务
all_jobs = job_service.list_jobs(status="running")

# 取消任务
job_service.cancel_job(job.job_id)
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
