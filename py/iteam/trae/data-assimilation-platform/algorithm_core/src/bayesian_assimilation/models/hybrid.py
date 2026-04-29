# algorithm_core/src/bayesian_assimilation/models/hybrid.py
# 混合同化算法实现

import numpy as np
from typing import Optional, Tuple, List
import logging

from ..core.base import AssimilationBase
from ..utils.config import BaseConfig
from .three_dimensional_var import ThreeDimensionalVAR
from .enkf import EnKF

logger = logging.getLogger(__name__)


class HybridAssimilation(AssimilationBase):
    """
    混合同化算法
    结合3D-VAR和EnKF的优势
    """
    
    def __init__(self, config: Optional[BaseConfig] = None):
        super().__init__(config)
        self.var = ThreeDimensionalVAR(config)
        self.enkf = EnKF(config)
        self.var_weight = 0.4  # 3D-VAR权重
        self.enkf_weight = 0.6  # EnKF权重
    
    def initialize_grid(self, domain_size: Tuple[float, float, float], 
                       resolution: Optional[float] = None):
        """
        初始化网格
        """
        super().initialize_grid(domain_size, resolution)
        self.var.grid_shape = self.grid_shape
        self.var.resolution = self.resolution
        self.enkf.grid_shape = self.grid_shape
        self.enkf.resolution = self.resolution
    
    def assimilate(self, background, observations, obs_locations, obs_errors=None):
        """
        执行混合同化
        
        Args:
            background: 背景场
            observations: 观测数据
            obs_locations: 观测位置
            obs_errors: 观测误差
            
        Returns:
            analysis: 分析场
            variance: 方差场
        """
        if self.grid_shape is None:
            raise RuntimeError("网格未初始化")
        
        logger.info(f"🚀 开始混合同化，网格: {self.grid_shape}")
        
        # 执行3D-VAR同化
        var_analysis, var_variance = self.var.assimilate(
            background, observations, obs_locations, obs_errors
        )
        
        # 执行EnKF同化
        enkf_analysis, enkf_variance = self.enkf.assimilate(
            background, observations, obs_locations, obs_errors
        )
        
        # 加权平均
        analysis = self.var_weight * var_analysis + self.enkf_weight * enkf_analysis
        variance = self.var_weight * var_variance + self.enkf_weight * enkf_variance
        
        self.analysis = analysis
        self.variance = variance
        
        logger.info("混合同化完成")
        return analysis, variance
    
    def set_weights(self, var_weight: float, enkf_weight: float):
        """
        设置权重
        
        Args:
            var_weight: 3D-VAR权重
            enkf_weight: EnKF权重
        """
        if var_weight + enkf_weight != 1.0:
            raise ValueError("权重之和必须为1.0")
        self.var_weight = var_weight
        self.enkf_weight = enkf_weight