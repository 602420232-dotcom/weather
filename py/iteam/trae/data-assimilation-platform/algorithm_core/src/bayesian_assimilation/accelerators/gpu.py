"""
GPU加速器
从 bayesian_assimilation(change).py 和 compatibility.py 提取
"""

import numpy as np
import logging
from typing import Optional, Tuple
try:
    import cupy as cp
    GPU_AVAILABLE = True
except ImportError:
    GPU_AVAILABLE = False
    cp = None

logger = logging.getLogger(__name__)


class GPUAccelerator:
    """GPU加速器"""
    
    def __init__(self, use_gpu: bool = True, gpu_memory_limit_gb: float = 8.0):
        self.use_gpu = use_gpu and GPU_AVAILABLE
        self.gpu_memory_limit_gb = gpu_memory_limit_gb
        self.gpu_memory = 0
        
        if self.use_gpu and cp is not None:
            mem_info = cp.cuda.Device(0).mem_info
            self.gpu_memory = mem_info[1] / 1e9
            logger.info(f"GPU加速可用: 显存: {self.gpu_memory:.2f}GB")
        else:
            logger.info("GPU不可用，将使用CPU计算")
    
    def to_gpu(self, array: np.ndarray) -> np.ndarray:
        """传输数据到GPU"""
        if self.use_gpu and cp is not None:
            return cp.asarray(array)
        return array
    
    def to_cpu(self, array) -> np.ndarray:
        """从GPU传回数据"""
        if self.use_gpu and cp is not None and hasattr(array, 'device'):
            return cp.asnumpy(array)
        return array
    
    def estimate_gpu_capacity(self, grid_shape: Tuple[int, int, int]) -> Tuple[int, int, int]:
        """估计GPU可处理的最大网格"""
        if not self.use_gpu or cp is None:
            return grid_shape
        
        nx, ny, nz = grid_shape
        n_total = nx * ny * nz
        
        # 估计内存需求
        estimated_memory_gb = n_total * 4 * 8 / 1e9
        
        if estimated_memory_gb > self.gpu_memory_limit_gb:
            scale_factor = (self.gpu_memory_limit_gb / estimated_memory_gb) ** (1/3)
            new_nx = int(nx * scale_factor)
            new_ny = int(ny * scale_factor)
            new_nz = int(nz * scale_factor)
            
            logger.warning(f"GPU内存不足，从{grid_shape}降采样到({new_nx}, {new_ny}, {new_nz})")
            return (new_nx, new_ny, new_nz)
        
        return grid_shape
    
    def gpu_matmul(self, A, B):
        """GPU矩阵乘法"""
        if self.use_gpu and cp is not None:
            if hasattr(A, 'dot'):
                return A.dot(B)
            else:
                return cp.dot(A, B)
        else:
            if hasattr(A, 'dot'):
                return A.dot(B)
            else:
                return np.dot(A, B)