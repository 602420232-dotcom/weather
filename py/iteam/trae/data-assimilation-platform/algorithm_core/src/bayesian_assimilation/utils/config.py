"""
配置类整合
"""

from dataclasses import dataclass, field
from typing import Optional, Tuple, List, Dict, Any, Union
import os


@dataclass
class BaseConfig:
    """基础配置（从文档3提取）"""
    method: str = "3DVAR"  # "3DVAR" 或 "EnKF"
    grid_resolution: float = 50.0
    update_interval: int = 300
    variance_threshold: float = 2.0
    background_error_scale: float = 1.5
    observation_error_scale: float = 0.8
    correlation_length: float = 50.0  # 默认相关长度，兼容所有子类
    max_cg_iterations: int = 200      # 默认CG迭代次数，兼容所有子类
    cg_tolerance: float = 1e-5        # 默认CG容忍度，兼容所有子类


@dataclass
class OptimizedConfig(BaseConfig):
    """优化配置（从文档1提取）"""
    correlation_length: float = 100.0
    use_sparse: bool = True
    max_cg_iterations: int = 10000
    cg_tolerance: float = 1e-10
    ensemble_size: int = 30


@dataclass
class AdaptiveConfig(OptimizedConfig):
    """自适应配置（从文档2提取）"""
    domain_size: Tuple[float, float, float] = (10000.0, 10000.0, 1000.0)
    target_resolution: float = 1.0
    use_gpu: bool = True
    use_incremental: bool = True
    use_block_parallel: bool = True
    
    # 自适应参数
    auto_resolution: bool = True
    min_resolution: float = 1.0
    max_resolution: float = 50.0
    resolution_decay: float = 0.9
    
    # 块分解参数
    block_size: int = 100
    block_overlap: int = 20
    n_workers: int = 4
    
    # 增量同化参数
    incremental_threshold: float = 0.1
    incremental_radius: int = 5
    
    # GPU参数
    gpu_memory_limit_gb: float = 8.0
    gpu_batch_size: int = 1000


# 兼容旧接口
AssimilationConfig = AdaptiveConfig


@dataclass
class CompatibleConfig(BaseConfig):
    """兼容性配置（从文档4提取）"""
    domain_size: Tuple[float, float, float] = (10000.0, 10000.0, 1000.0)
    correlation_length: float = 50.0
    use_gpu: bool = True
    use_incremental: bool = True
    use_block_parallel: bool = True
    max_cg_iterations: int = 200
    cg_tolerance: float = 1e-5
    block_size: int = 100
    block_overlap: int = 10
    n_workers: int = 2
    incremental_threshold: float = 0.1
    incremental_radius: int = 3
    gpu_memory_limit_gb: float = 4.0


# 配置工厂
class ConfigFactory:
    """配置工厂，创建不同类型的配置"""
    
    @staticmethod
    def create(config_type: str = "adaptive", **kwargs) -> BaseConfig:
        """创建配置实例"""
        config_classes = {
            "base": BaseConfig,
            "optimized": OptimizedConfig,
            "adaptive": AdaptiveConfig,
            "compatible": CompatibleConfig
        }
        
        if config_type not in config_classes:
            raise ValueError(f"未知的配置类型: {config_type}")
        
        return config_classes[config_type](**kwargs)
    
    @staticmethod
    def from_dict(config_dict: Dict[str, Any]) -> BaseConfig:
        """从字典创建配置"""
        config_type = config_dict.get("config_type", "adaptive")
        
        # 移除config_type字段
        if "config_type" in config_dict:
            config_dict = config_dict.copy()
            del config_dict["config_type"]
        
        return ConfigFactory.create(config_type, **config_dict)


# 环境变量配置
def get_config_from_env() -> BaseConfig:
    """从环境变量获取配置"""
    import os
    
    config_type = os.getenv("ASSIMILATION_CONFIG_TYPE", "adaptive")
    
    # 基础配置
    config_data = {
        "method": os.getenv("ASSIMILATION_METHOD", "3DVAR"),
        "grid_resolution": float(os.getenv("GRID_RESOLUTION", "50.0")),
        "use_gpu": os.getenv("USE_GPU", "True").lower() == "true"
    }
    
    return ConfigFactory.create(config_type, **config_data)