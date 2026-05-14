# routes

FastAPI 路由模块，定义微服务的所有 HTTP API 端点，按功能域划分路由文件。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：`assimilation`、`batch`、`monitoring`、`variance_field` |
| `assimilation.py` | 同化路由：`POST /assimilate`，提交单一同化任务 |
| `batch.py` | 批量路由：`POST /batch`，提交批量同化任务 |
| `monitoring.py` | 监控路由：`GET /status`、`GET /metrics`，系统运行状态 |
| `variance_field.py` | 方差场路由：方差场计算与查询 |
| `test_assimilation.py` | 同化路由测试 |
| `test_batch.py` | 批量路由测试 |

## API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v1/assimilation/assimilate` | 提交单一同化任务 |
| `POST` | `/api/v1/assimilation/batch` | 提交批量同化任务 |
| `GET` | `/api/v1/assimilation/status/{job_id}` | 查询任务状态 |
| `GET` | `/api/v1/assimilation/result/{job_id}` | 获取任务结果 |
| `GET` | `/health` | 全局健康检查 |

## 路由注册

在 `main.py` 中通过 `app.include_router()` 注册：

```python
app.include_router(assimilation.router, prefix="/api/v1/assimilation", tags=["assimilation"])
```

## 扩展

如需新增功能路由：
1. 在 `routes/` 下创建新 `.py` 文件
2. 定义 `router = APIRouter()`
3. 在 `__init__.py` 中导出
4. 在 `main.py` 中注册

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
