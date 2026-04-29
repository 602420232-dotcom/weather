# service_python/src/api/models/response.py

from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

class AssimilationResponse(BaseModel):
    """同化响应模型"""
    job_id: str = Field(..., description="作业ID")
    status: str = Field(..., description="状态")
    analysis_field: List[List[List[float]]] = Field(..., description="分析场")
    variance_field: List[List[List[float]]] = Field(..., description="方差场")
    computation_time: float = Field(..., description="计算时间（秒）")
    timestamp: float = Field(..., description="时间戳")
    message: Optional[str] = Field(None, description="附加信息")
    
    class Config:
        schema_extra = {
            "example": {
                "job_id": "job_123456",
                "status": "SUCCESS",
                "analysis_field": [[[1.2, 2.1], [3.1, 4.2]], [[5.1, 6.2], [7.1, 8.2]]],
                "variance_field": [[[0.1, 0.2], [0.1, 0.2]], [[0.1, 0.2], [0.1, 0.2]]],
                "computation_time": 0.5,
                "timestamp": 1620000000.0,
                "message": "同化计算成功"
            }
        }

class BatchResponse(BaseModel):
    """批量响应模型"""
    completed: int = Field(..., description="完成的作业数")
    total: int = Field(..., description="总作业数")
    status: str = Field(..., description="整体状态")
    
    class Config:
        schema_extra = {
            "example": {
                "completed": 2,
                "total": 2,
                "status": "SUCCESS"
            }
        }

class StatusResponse(BaseModel):
    """状态响应模型"""
    job_id: str = Field(..., description="作业ID")
    status: str = Field(..., description="状态")
    message: Optional[str] = Field(None, description="附加信息")
    timestamp: float = Field(..., description="时间戳")
    
    class Config:
        schema_extra = {
            "example": {
                "job_id": "job_123456",
                "status": "COMPLETED",
                "message": "作业已完成",
                "timestamp": 1620000000.0
            }
        }