# algorithm_core/src/bayesian_assimilation/models/three_dimensional_var.py
# 基于 compatibility.py 重构，适配项目结构

import numpy as np
from scipy.sparse import csr_matrix
from scipy.sparse.linalg import LinearOperator, cg
from dataclasses import dataclass
from typing import Optional, Tuple, List
import logging

from ..core.base import AssimilationBase
from ..utils.config import BaseConfig

logger = logging.getLogger(__name__)


class ThreeDimensionalVAR(AssimilationBase):
    """
    3DVAR同化算法 - 生产级实现
    源自: compatibility.py (GPU加速贝叶斯同化系统 - 最终稳定版)
    """
    
    def __init__(self, config: Optional[BaseConfig] = None):
        super().__init__(config)
        self.gpu = None  # 稍后注入GPUAccelerator
        
    def assimilate(self, background, observations, obs_locations, obs_errors=None):
        """主入口：执行同化"""
        if self.grid_shape is None:
            raise RuntimeError("网格未初始化")
        
        logger.info(f"🚀 开始3DVAR同化，网格: {self.grid_shape}")
        return self._assimilate_optimized(background, observations, obs_locations, obs_errors)
    
    def _assimilate_optimized(self, bg, obs, obs_loc, obs_err):
        """优化版同化（源自compatibility.py的smart_assimilate）"""
        nx, ny, nz = self.grid_shape
        n = nx * ny * nz
        
        # 构建观测算子
        H = self._build_observation_operator_trilinear(obs_loc)
        
        # 构建协方差
        # 暂时使用简化的协方差矩阵
        # from .covariance import FastSparseBackgroundCovariance  # 稍后创建
        # cov = FastSparseBackgroundCovariance(
        #     self.grid_shape, 
        #     self.resolution,
        #     self.config.background_error_scale,
        #     self.config.correlation_length
        # )
        
        # 简化的协方差实现
        class SimpleCovariance:
            def __init__(self, grid_shape, resolution):
                self.grid_shape = grid_shape
                self.resolution = resolution
                self.variance = 1.0
            
            def apply_inverse(self, x):
                return x / self.variance
        
        cov = SimpleCovariance(self.grid_shape, self.resolution)
        
        # 线性算子包装
        B_inv = LinearOperator(
            (n, n), 
            matvec=cov.apply_inverse
        )
        
        # 观测误差
        obs_err = obs_err or np.full(len(obs), 0.1)  # 默认观测误差
        from scipy.sparse import diags
        R_inv = diags(1.0 / (obs_err**2 + 1e-6))
        
        # Hessian矩阵
        def hessian_matvec(x):
            return cov.apply_inverse(x) + H.T @ (R_inv @ (H @ x))
        
        # 右侧向量
        xb = bg.ravel()
        b = cov.apply_inverse(xb) + H.T @ (R_inv @ obs)
        
        # 共轭梯度求解
        A = LinearOperator((n, n), matvec=hessian_matvec)
        xa, info = cg(A, b, x0=xb, maxiter=100, 
                     atol=1e-6)  # 默认参数
        
        if info != 0:
            logger.warning(f"CG未收敛: {info}")
        
        # 方差估计（简化版）
        variance = self._estimate_variance_diagonal(cov, H, R_inv)
        
        return xa.reshape(nx, ny, nz), variance.reshape(nx, ny, nz)
    
    def _build_observation_operator_trilinear(self, obs_loc):
        """三线性插值观测算子（源自compatibility.py）"""
        nx, ny, nz = self.grid_shape
        rows, cols, vals = [], [], []
        
        for i, (x, y, z) in enumerate(obs_loc):
            # 归一化到网格索引
            ix = min(max(0, int(x/self.resolution)), nx-2)
            iy = min(max(0, int(y/self.resolution)), ny-2)
            iz = min(max(0, int(z/self.resolution)), nz-2)
            
            # 计算插值权重
            dx = (x/self.resolution - ix)
            dy = (y/self.resolution - iy)
            dz = (z/self.resolution - iz)
            
            # 三线性插值权重
            weights = [
                (1-dx)*(1-dy)*(1-dz),
                dx*(1-dy)*(1-dz),
                (1-dx)*dy*(1-dz),
                dx*dy*(1-dz),
                (1-dx)*(1-dy)*dz,
                dx*(1-dy)*dz,
                (1-dx)*dy*dz,
                dx*dy*dz
            ]
            
            # 8个相邻网格点
            indices = [
                (ix, iy, iz),
                (ix+1, iy, iz),
                (ix, iy+1, iz),
                (ix+1, iy+1, iz),
                (ix, iy, iz+1),
                (ix+1, iy, iz+1),
                (ix, iy+1, iz+1),
                (ix+1, iy+1, iz+1)
            ]
            
            # 添加非零元素
            for idx, (di, dj, dk) in enumerate(indices):
                if weights[idx] > 1e-6:
                    flat_idx = di * ny * nz + dj * nz + dk
                    rows.append(i)
                    cols.append(flat_idx)
                    vals.append(weights[idx])
        
        return csr_matrix((vals, (rows, cols)), shape=(len(obs_loc), nx*ny*nz))
    
    def _estimate_variance_diagonal(self, cov, H, R_inv):
        """对角方差估计"""
        n = self.nx * self.ny * self.nz
        diag = np.zeros(n)
        
        # 背景项
        diag += 1.0 / (cov.variance + 1e-6)
        
        # 观测项
        for i in range(n):
            obs_idx = H[:, i].nonzero()[0]
            if len(obs_idx) > 0:
                h_vals = H[obs_idx, i].toarray().flatten()
                r_diag = R_inv.diagonal()[obs_idx]
                diag[i] += np.sum(h_vals**2 * r_diag)
        
        return 1.0 / (diag + 1e-6)
