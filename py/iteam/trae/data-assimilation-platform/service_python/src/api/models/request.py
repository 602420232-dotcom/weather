# service_python/src/api/models/request.py

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import numpy as np

class AssimilationRequest(BaseModel):
    """同化请求模型"""
    job_id: str = Field(..., description="作业ID")
    background_field: List[List[List[float]]] = Field(..., description="背景场")
    observations: List[float] = Field(..., description="观测数据")
    obs_locations: Optional[List[List[float]]] = Field(None, description="观测位置")
    config: Optional[Dict[str, Any]] = Field({}, description="配置参数")
    allow_degraded: bool = Field(False, description="是否允许降级")
    algorithm: str = Field("3dvar", description="算法选择: 3dvar, enhanced")
    
    class Config:
        schema_extra = {
            "example": {
                "job_id": "job_123456",
                "background_field": [[[1.0, 2.0], [3.0, 4.0]], [[5.0, 6.0], [7.0, 8.0]]],
                "observations": [1.5, 2.5, 3.5],
                "obs_locations": [[0, 0, 0], [1, 1, 0], [0, 1, 0]],
                "config": {
                    "method": "3DVAR",
                    "grid_resolution": 50.0
                },
                "allow_degraded": False,
                "algorithm": "3dvar"
            }
        }

class BatchRequest(BaseModel):
    """批量请求模型"""
    jobs: List[AssimilationRequest] = Field(..., description="作业列表")
    
    class Config:
        schema_extra = {
            "example": {
                "jobs": [
                    {
                        "job_id": "job_1",
                        "background_field": [[[1.0, 2.0], [3.0, 4.0]], [[5.0, 6.0], [7.0, 8.0]]],
                        "observations": [1.5, 2.5],
                        "obs_locations": [[0, 0, 0], [1, 1, 0]],
                        "config": {}
                    },
                    {
                        "job_id": "job_2",
                        "background_field": [[[1.0, 2.0], [3.0, 4.0]], [[5.0, 6.0], [7.0, 8.0]]],
                        "observations": [3.5, 4.5],
                        "obs_locations": [[0, 1, 0], [1, 0, 0]],
                        "config": {}
                    }
                ]
            }
        }