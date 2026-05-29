from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any


class AssimilationRequest(BaseModel):
    """同化请求模型"""
    background: Dict[str, Any]
    observations: Dict[str, Any]
    config: Optional[Dict[str, Any]] = None


class QualityControlRequest(BaseModel):
    """质量控制请求模型"""
    data: Dict[str, Any]
    data_type: str = 'all'


class RiskAssessmentRequest(BaseModel):
    """风险评估请求模型"""
    wind_speed: List[List[List[float]]]
    variance: Optional[List[List[List[float]]]] = None


class SelfImproveRequest(BaseModel):
    job_id: str = Field(..., description="任务ID")
    X: List[List[float]] = Field(..., description="输入特征数据")
    y: List[float] = Field(..., description="目标标签数据")
    epochs: int = Field(default=20, ge=1, le=500, description="训练轮次")
    batch_size: int = Field(default=32, ge=1, le=1024, description="批次大小")
