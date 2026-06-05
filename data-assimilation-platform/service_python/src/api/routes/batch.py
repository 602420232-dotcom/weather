# service_python/src/api/routes/batch.py

from fastapi import APIRouter, HTTPException
from api.models.request import BatchRequest  # type: ignore[import-not-found]
from api.models.response import BatchResponse  # type: ignore[import-not-found]
from api.core.assimilation_service import AssimilationService
import logging

logger = logging.getLogger(__name__)

router = APIRouter()

# 全局服务实例
assimilation_service = None


def set_assimilation_service(service: AssimilationService):
    """设置同化服务实例"""
    global assimilation_service
    assimilation_service = service


@router.post("/batch", response_model=BatchResponse)
def batch_assimilation(request: BatchRequest):
    """
    批量同化计算

    用于历史数据回放和批量处理
    """
    try:
        # 检查服务实例
        if not assimilation_service:
            raise HTTPException(status_code=503, detail="服务未初始化")

        # 构建请求列表
        requests = []
        for job in request.jobs:
            request_dict = {
                "job_id": job.job_id,
                "background_field": job.background_field,
                "observations": job.observations,
                "obs_locations": job.obs_locations,
                "config": job.config,
                "allow_degraded": job.allow_degraded
            }
            requests.append(request_dict)

        # 处理请求
        results = []
        for req in requests:
            try:
                result = assimilation_service.execute(
                    algorithm=req.get("config", {}).get("algorithm", "3dvar"),
                    background=req["background_field"],
                    observations=req["observations"],
                    config=req.get("config"),
                )
                results.append(result)
            except Exception as e:
                results.append(e)

        # 统计结果
        completed = len([r for r in results if not isinstance(r, Exception)])
        total = len(results)

        logger.info(f"批量处理完成，成功: {completed}/{total}")

        return BatchResponse(
            completed=completed,
            total=total,
            status="SUCCESS" if completed == total else "PARTIAL"
        )

    except Exception as e:
        logger.error(f"批量处理失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
