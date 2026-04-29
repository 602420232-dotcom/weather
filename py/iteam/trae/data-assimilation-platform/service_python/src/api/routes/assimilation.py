# service_python/src/api/routes/assimilation.py

from fastapi import APIRouter, HTTPException, BackgroundTasks
from api.models.request import AssimilationRequest
from api.models.response import AssimilationResponse
from api.core.assimilation_service import AssimilationService
from api.utils.validators import validate_grid_consistency, validate_observation_locations
import time
import logging
import numpy as np

logger = logging.getLogger(__name__)

router = APIRouter()

# 全局服务实例
assimilation_service = None

def set_assimilation_service(service: AssimilationService):
    """设置同化服务实例"""
    global assimilation_service
    assimilation_service = service

@router.post("/compute", response_model=AssimilationResponse)
async def compute_assimilation(
    request: AssimilationRequest,
    background_tasks: BackgroundTasks
):
    """
    执行贝叶斯同化计算
    
    - 支持3DVAR/EnKF/4DVAR算法
    - 自动并行处理大网格
    - 异步持久化结果
    """
    start_time = time.time()
    
    try:
        # 参数校验
        validate_grid_consistency(request.background_field, request.observations)
        if request.obs_locations:
            validate_observation_locations(request.obs_locations, request.observations)
        
        # 检查服务实例
        if not assimilation_service:
            raise HTTPException(status_code=503, detail="服务未初始化")
        
        # 构建请求字典
        request_dict = {
            "job_id": request.job_id,
            "background_field": request.background_field,
            "observations": request.observations,
            "obs_locations": request.obs_locations,
            "config": request.config,
            "allow_degraded": request.allow_degraded,
            "algorithm": request.algorithm  # 新增算法选择
        }
        
        # 执行同化计算
        result = await assimilation_service.compute(request_dict)
        
        # 异步保存结果
        background_tasks.add_task(
            persist_result,
            job_id=request.job_id,
            result=result
        )
        
        elapsed = time.time() - start_time
        logger.info(f"同化计算完成，Job: {request.job_id}, 耗时: {elapsed:.2f}s")
        
        return AssimilationResponse(
            job_id=request.job_id,
            status="SUCCESS",
            analysis_field=result.analysis.tolist(),
            variance_field=result.variance.tolist(),
            computation_time=elapsed,
            timestamp=time.time(),
            message="同化计算成功"
        )
        
    except Exception as e:
        logger.error(f"同化计算失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/self-improve")
async def self_improve_model(
    job_id: str,
    X: list,
    y: list,
    epochs: int = 20,
    batch_size: int = 32
):
    """
    自迭代改进模型
    
    - 支持增强贝叶斯同化器的自迭代改进
    - 使用新数据持续优化模型性能
    """
    try:
        # 检查服务实例
        if not assimilation_service:
            raise HTTPException(status_code=503, detail="服务未初始化")
        
        # 执行自迭代改进
        result = await assimilation_service.self_improve(
            job_id=job_id,
            X=np.array(X),
            y=np.array(y),
            epochs=epochs,
            batch_size=batch_size
        )
        
        logger.info(f"模型自迭代改进完成，Job: {job_id}")
        
        return {
            "status": "SUCCESS",
            "job_id": job_id,
            "result": result,
            "timestamp": time.time()
        }
        
    except Exception as e:
        logger.error(f"自迭代改进失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/model-performance/{job_id}")
async def get_model_performance(job_id: str):
    """
    获取模型性能
    
    - 支持增强贝叶斯同化器的性能查询
    - 返回训练历史和最佳分数
    """
    try:
        # 检查服务实例
        if not assimilation_service:
            raise HTTPException(status_code=503, detail="服务未初始化")
        
        # 获取模型性能
        performance = assimilation_service.get_model_performance(job_id)
        
        logger.info(f"获取模型性能完成，Job: {job_id}")
        
        return {
            "status": "SUCCESS",
            "job_id": job_id,
            "performance": performance,
            "timestamp": time.time()
        }
        
    except Exception as e:
        logger.error(f"获取模型性能失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

async def persist_result(job_id: str, result):
    """异步持久化结果"""
    # 这里可以实现结果的持久化逻辑
    # 例如保存到数据库或对象存储
    logger.info(f"持久化结果，JobID: {job_id}")
    pass