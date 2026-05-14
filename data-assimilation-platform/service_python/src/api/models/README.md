# models

Python 微服务的数据模型层，使用 Pydantic v2 定义 API 请求和响应的数据结构。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：请求/响应模型类 |
| `request.py` | 请求模型：`AssimilationRequest`（同化请求）、`QualityControlRequest`、`RiskAssessmentRequest`、`SelfImproveRequest` |
| `response.py` | 响应模型：`AssimilationResponse`（同化结果）、`HealthResponse`（健康检查） |

## 请求模型

### AssimilationRequest

```python
class AssimilationRequest(BaseModel):
    algorithm: str                    # "3dvar" | "4dvar" | "enkf" | "hybrid"
    background: dict                  # {grid, variables}
    observations: list[dict]          # [{lat, lon, value, error}]
    config: dict | None               # {max_iterations, tolerance, parallel}
```

### SelfImproveRequest

```python
class SelfImproveRequest(BaseModel):
    job_id: str                       # 任务ID
    X: list[list[float]]              # 输入特征
    y: list[float]                    # 目标标签
    epochs: int                       # 训练轮次 (1-500)
    batch_size: int                   # 批次大小 (1-1024)
```

## 响应模型

### AssimilationResponse

```python
class AssimilationResponse(BaseModel):
    status: str                       # "success" | "error"
    analysis: Any | None              # 分析场数据
    variance: Any | None              # 方差场数据
    metrics: dict | None              # 性能指标
    message: str | None               # 附加信息
```

### HealthResponse

```python
class HealthResponse(BaseModel):
    status: str                       # "healthy" | "degraded"
    version: str                      # 服务版本
    gpu_available: bool               # GPU 可用状态
    memory_usage: str | None          # 内存使用
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
