# service_python/src/api/core/assimilation_service.py

import asyncio
import logging
import numpy as np
import sys
import os
from typing import Optional, Dict, Any
from dataclasses import dataclass

# 添加算法核心路径
sys.path.append(os.path.join(os.path.dirname(__file__), '../../../../algorithm_core/src'))

from bayesian_assimilation.core.assimilator import OptimizedAssimilator
from bayesian_assimilation.models.enhanced_bayesian import EnhancedBayesianAssimilation
from bayesian_assimilation.utils.config import ConfigFactory
from api.parallel.dask import DaskClusterManager

logger = logging.getLogger(__name__)

@dataclass
class AssimilationResult:
    """同化结果"""
    analysis: np.ndarray
    variance: np.ndarray
    job_id: str
    computation_time: float

class AssimilationService:
    """同化服务"""
    
    def __init__(self, cluster_manager: Optional[DaskClusterManager] = None):
        self.cluster_manager = cluster_manager
        self.assimilators = {}
    
    async def compute(self, request: Dict[str, Any]) -> AssimilationResult:
        """执行同化计算"""
        import time
        start_time = time.time()
        
        try:
            # 提取参数
            job_id = request.get('job_id', f'job_{int(time.time())}')
            background_field = np.array(request.get('background_field'))
            observations = np.array(request.get('observations'))
            obs_locations = np.array(request.get('obs_locations', []))
            config_data = request.get('config', {})
            algorithm = request.get('algorithm', '3dvar')  # 新增算法选择
            
            # 创建配置
            config = ConfigFactory.from_dict(config_data)
            
            # 初始化同化器
            if job_id not in self.assimilators:
                if algorithm == 'enhanced':
                    self.assimilators[job_id] = EnhancedBayesianAssimilation(config)
                else:
                    self.assimilators[job_id] = OptimizedAssimilator(config)
            
            assimilator = self.assimilators[job_id]
            
            # 初始化网格
            grid_shape = background_field.shape
            if hasattr(assimilator, 'initialize_grid'):
                assimilator.initialize_grid((grid_shape[0] * config.grid_resolution, 
                                          grid_shape[1] * config.grid_resolution, 
                                          grid_shape[2] * config.grid_resolution),
                                         config.grid_resolution)
            else:
                assimilator.grid_shape = grid_shape
                assimilator.resolution = config.grid_resolution
            
            # 执行同化
            if algorithm == 'enhanced' and isinstance(assimilator, EnhancedBayesianAssimilation):
                analysis, variance = assimilator.assimilate(
                    background_field, 
                    observations, 
                    obs_locations
                )
            else:
                analysis, variance = assimilator.assimilate_3dvar_optimized(
                    background_field, 
                    observations, 
                    obs_locations
                )
            
            computation_time = time.time() - start_time
            
            logger.info(f"同化计算完成，Job: {job_id}, 算法: {algorithm}, 耗时: {computation_time:.2f}s")
            
            return AssimilationResult(
                analysis=analysis,
                variance=variance,
                job_id=job_id,
                computation_time=computation_time
            )
            
        except Exception as e:
            logger.error(f"同化计算失败: {str(e)}", exc_info=True)
            raise
    
    def queue_size(self) -> int:
        """获取队列大小"""
        return len(self.assimilators)
    
    async def batch_compute(self, requests: list) -> list:
        """批量计算"""
        tasks = [self.compute(request) for request in requests]
        results = await asyncio.gather(*tasks, return_exceptions=True)
        return results
    
    async def self_improve(self, job_id: str, X: np.ndarray, y: np.ndarray, epochs: int = 20, batch_size: int = 32) -> Dict[str, Any]:
        """自迭代改进模型"""
        try:
            if job_id not in self.assimilators:
                raise ValueError(f"Job ID {job_id} 不存在")
            
            assimilator = self.assimilators[job_id]
            
            if not isinstance(assimilator, EnhancedBayesianAssimilation):
                raise ValueError(f"只有增强贝叶斯同化器支持自迭代改进")
            
            # 执行自迭代改进
            result = assimilator.self_improve(X, y, epochs, batch_size)
            
            logger.info(f"模型自迭代改进完成，Job: {job_id}")
            
            return result
            
        except Exception as e:
            logger.error(f"自迭代改进失败: {str(e)}", exc_info=True)
            raise
    
    def get_model_performance(self, job_id: str) -> Dict[str, Any]:
        """获取模型性能"""
        try:
            if job_id not in self.assimilators:
                raise ValueError(f"Job ID {job_id} 不存在")
            
            assimilator = self.assimilators[job_id]
            
            if not isinstance(assimilator, EnhancedBayesianAssimilation):
                raise ValueError(f"只有增强贝叶斯同化器支持性能查询")
            
            return assimilator.get_model_performance()
            
        except Exception as e:
            logger.error(f"获取模型性能失败: {str(e)}", exc_info=True)
            raise