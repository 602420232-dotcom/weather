# service_python/src/api/utils/validators.py

import numpy as np
from typing import List, Any


def validate_grid_consistency(background_field: List[List[List[float]]], 
                           observations: List[float]) -> None:
    """
    验证网格一致性
    
    Args:
        background_field: 背景场
        observations: 观测数据
        
    Raises:
        ValueError: 如果网格不一致
    """
    # 检查背景场维度
    if not background_field:
        raise ValueError("背景场不能为空")
    
    # 检查观测数据
    if not observations:
        raise ValueError("观测数据不能为空")
    
    # 检查背景场维度
    bg_array = np.array(background_field)
    if bg_array.ndim != 3:
        raise ValueError(f"背景场必须是3维数组，当前维度: {bg_array.ndim}")
    
    # 检查观测数据长度
    if len(observations) == 0:
        raise ValueError("观测数据长度不能为0")
    
    # 可以添加更多验证逻辑
    

def validate_observation_locations(obs_locations: List[List[float]], 
                                observations: List[float]) -> None:
    """
    验证观测位置
    
    Args:
        obs_locations: 观测位置
        observations: 观测数据
        
    Raises:
        ValueError: 如果观测位置与观测数据不一致
    """
    if obs_locations is None:
        return
    
    if len(obs_locations) != len(observations):
        raise ValueError(
            f"观测位置数量与观测数据数量不一致: {len(obs_locations)} vs {len(observations)}"
        )
    
    # 检查每个观测位置的维度
    for i, loc in enumerate(obs_locations):
        if len(loc) != 3:
            raise ValueError(
                f"观测位置 {i} 必须是3维坐标，当前维度: {len(loc)}"
            )
    

def validate_config(config: dict) -> dict:
    """
    验证配置
    
    Args:
        config: 配置字典
        
    Returns:
        验证后的配置
    """
    # 基本验证
    if not isinstance(config, dict):
        config = {}
    
    # 添加默认值
    default_config = {
        "method": "3DVAR",
        "grid_resolution": 50.0,
        "background_error_scale": 1.5,
        "observation_error_scale": 0.8
    }
    
    # 合并配置
    for key, value in default_config.items():
        if key not in config:
            config[key] = value
    
    return config