from pydantic import BaseModel
from typing import Optional, Any


class AssimilationResponse(BaseModel):
    status: str
    analysis: Optional[Any] = None
    variance: Optional[Any] = None
    metrics: Optional[dict] = None
    message: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    version: str
    gpu_available: bool
    memory_usage: Optional[str] = None
