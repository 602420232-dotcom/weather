# algorithm_core/src/bayesian_assimilation/strategies/base.py

from abc import ABC, abstractmethod
from typing import Tuple, Optional
import numpy as np


class AssimilationStrategy(ABC):
    """同化执行策略抽象基类"""
    
    def __init__(self, context):
        self.context = context  # AssimilationContext
    
    @abstractmethod
    def execute(self, 
                background: np.ndarray,
                observations: np.ndarray,
                obs_locations: np.ndarray,
                obs_errors: Optional[np.ndarray] = None) -> Tuple[np.ndarray, np.ndarray]:
        """执行同化，返回 (analysis, variance)"""
        pass
    
    @property
    @abstractmethod
    def name(self) -> str:
        """策略名称"""
        pass
    
    @property
    def supports_incremental(self) -> bool:
        """是否支持增量模式"""
        return False
    
    @property
    def supports_parallel(self) -> bool:
        """是否支持并行"""
        return False
