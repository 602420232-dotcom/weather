#!/usr/bin/env python3
"""
增强的贝叶斯同化模型
添加机器学习和深度学习部分，允许自迭代
"""

import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Sequential, load_model
from tensorflow.keras.layers import Dense, LSTM, Dropout, Input
from tensorflow.keras.optimizers import Adam
from scipy.sparse import csr_matrix, diags
from scipy.sparse.linalg import LinearOperator, cg
from dataclasses import dataclass
from typing import Optional, Tuple, List, Dict, Any
import logging
import os
from datetime import datetime

from ..core.base import AssimilationBase
from ..utils.config import BaseConfig

logger = logging.getLogger(__name__)

class EnhancedBayesianAssimilation(AssimilationBase):
    """
    增强的贝叶斯同化模型
    添加机器学习和深度学习部分，允许自迭代
    """
    
    def __init__(self, config: Optional[BaseConfig] = None, model_path=None):
        super().__init__(config)
        self.model_path = model_path or os.path.join(os.path.dirname(__file__), 'models')
        os.makedirs(self.model_path, exist_ok=True)
        self.ml_model = None  # 机器学习模型
        self.dl_model = None  # 深度学习模型
        self.history = []  # 训练历史
        self.best_score = float('inf')  # 最佳模型分数
    
    def assimilate(self, background, observations, obs_locations, obs_errors=None):
        """
        执行增强的贝叶斯同化
        """
        if self.grid_shape is None:
            raise RuntimeError("网格未初始化")
        
        logger.info(f"🚀 开始增强贝叶斯同化，网格: {self.grid_shape}")
        
        # 1. 执行传统3DVAR同化
        analysis, variance = self._assimilate_3dvar(background, observations, obs_locations, obs_errors)
        
        # 2. 使用机器学习模型进行优化
        if self.ml_model or self.dl_model:
            analysis, variance = self._enhance_with_ml(analysis, variance, background, observations, obs_locations)
        
        self.analysis = analysis
        self.variance = variance
        
        return analysis, variance
    
    def _assimilate_3dvar(self, background, observations, obs_locations, obs_errors=None):
        """
        执行3DVAR同化
        """
        nx, ny, nz = self.grid_shape
        n = nx * ny * nz
        
        # 构建观测算子
        H = self._build_observation_operator(obs_locations)
        
        # 构建协方差
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
        obs_errors = obs_errors or np.full(len(observations), 0.1)  # 默认观测误差
        R_inv = diags(1.0 / (obs_errors**2 + 1e-6))
        
        # Hessian矩阵
        def hessian_matvec(x):
            return cov.apply_inverse(x) + H.T @ (R_inv @ (H @ x))
        
        # 右侧向量
        xb = background.ravel()
        b = cov.apply_inverse(xb) + H.T @ (R_inv @ observations)
        
        # 共轭梯度求解
        A = LinearOperator((n, n), matvec=hessian_matvec, dtype=np.float64)
        xa, info = cg(A, b, x0=xb, maxiter=100, atol=1e-6)
        
        if info != 0:
            logger.warning(f"CG未收敛: {info}")
        
        # 方差估计
        variance = self._estimate_variance(cov, H, R_inv)
        
        return xa.reshape(nx, ny, nz), variance.reshape(nx, ny, nz)
    
    def _build_observation_operator(self, obs_locations):
        """
        构建观测算子
        """
        nx, ny, nz = self.grid_shape
        rows, cols, vals = [], [], []
        
        for i, (x, y, z) in enumerate(obs_locations):
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
        
        return csr_matrix((vals, (rows, cols)), shape=(len(obs_locations), nx*ny*nz))
    
    def _estimate_variance(self, cov, H, R_inv):
        """
        估计方差
        """
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
    
    def _enhance_with_ml(self, analysis, variance, background, observations, obs_locations):
        """
        使用机器学习模型增强同化结果
        """
        try:
            # 准备输入数据
            input_data = self._prepare_ml_input(analysis, variance, background, observations, obs_locations)
            
            # 使用深度学习模型
            if self.dl_model:
                enhanced_analysis = self.dl_model.predict(input_data)
                enhanced_analysis = enhanced_analysis.reshape(analysis.shape)
                
                # 调整方差
                enhanced_variance = variance * 0.9  # 减少方差，表示更有信心
            else:
                # 使用机器学习模型
                if self.ml_model:
                    enhanced_analysis = self.ml_model.predict(input_data)
                    enhanced_analysis = enhanced_analysis.reshape(analysis.shape)
                    enhanced_variance = variance * 0.95
                else:
                    # 没有模型，返回原始结果
                    return analysis, variance
            
            logger.info("使用机器学习模型增强同化结果")
            return enhanced_analysis, enhanced_variance
            
        except Exception as e:
            logger.error(f"机器学习增强失败: {e}")
            return analysis, variance
    
    def _prepare_ml_input(self, analysis, variance, background, observations, obs_locations):
        """
        准备机器学习输入数据
        """
        # 提取特征
        analysis_mean = np.mean(analysis)
        analysis_std = np.std(analysis)
        variance_mean = np.mean(variance)
        variance_std = np.std(variance)
        background_mean = np.mean(background)
        background_std = np.std(background)
        observations_mean = np.mean(observations)
        observations_std = np.std(observations)
        
        # 构建特征向量
        features = np.array([
            analysis_mean, analysis_std,
            variance_mean, variance_std,
            background_mean, background_std,
            observations_mean, observations_std,
            len(observations)  # 观测数量
        ])
        
        # 扩展维度以适应模型输入
        return features.reshape(1, -1)
    
    def build_dl_model(self, input_shape):
        """
        构建深度学习模型
        """
        if self.grid_shape is None:
            raise RuntimeError("网格未初始化")
        
        model = Sequential()
        model.add(Input(shape=input_shape))
        model.add(Dense(64, activation='relu'))
        model.add(Dropout(0.2))
        model.add(Dense(32, activation='relu'))
        model.add(Dropout(0.2))
        model.add(Dense(np.prod(list(self.grid_shape)), activation='linear'))
        model.compile(optimizer=Adam(learning_rate=0.001), loss='mse')
        
        self.dl_model = model
        logger.info(f"构建深度学习模型，输入形状: {input_shape}")
        return model
    
    def train_ml_model(self, X, y, epochs=50, batch_size=32):
        """
        训练机器学习模型
        """
        try:
            # 构建模型
            if not self.dl_model:
                self.build_dl_model(X.shape[1:])
            
            # 训练模型
            if self.dl_model:
                history = self.dl_model.fit(X, y, epochs=epochs, batch_size=batch_size, validation_split=0.2, verbose=1)
                
                # 保存训练历史
                self.history.append({
                    'timestamp': datetime.now().isoformat(),
                    'loss': history.history['loss'][-1],
                    'val_loss': history.history['val_loss'][-1],
                    'epochs': epochs
                })
                
                # 保存最佳模型
                current_loss = history.history['val_loss'][-1]
                if current_loss < self.best_score:
                    self.best_score = current_loss
                    model_path = os.path.join(self.model_path, 'enhanced_bayesian_model.h5')
                    self.dl_model.save(model_path)
                    logger.info(f"最佳模型已保存，验证损失: {current_loss}")
                
                logger.info("机器学习模型训练完成")
                return {
                    'success': True,
                    'loss': history.history['loss'][-1],
                    'val_loss': history.history['val_loss'][-1]
                }
            else:
                logger.error("无法训练模型，模型未初始化")
                return {
                    'success': False,
                    'error': '模型未初始化'
                }
            
        except Exception as e:
            logger.error(f"训练机器学习模型失败: {e}")
            return {
                'success': False,
                'error': str(e)
            }
    
    def load_model(self):
        """
        加载模型
        """
        model_path = os.path.join(self.model_path, 'enhanced_bayesian_model.h5')
        if os.path.exists(model_path):
            self.dl_model = load_model(model_path)
            logger.info(f"模型加载成功: {model_path}")
        else:
            logger.warning(f"模型文件不存在: {model_path}")
    
    def self_improve(self, X, y, epochs=20, batch_size=32):
        """
        自迭代改进模型
        """
        try:
            logger.info("开始自迭代改进模型...")
            
            # 加载现有模型
            if not self.dl_model:
                self.load_model()
            
            # 继续训练
            result = self.train_ml_model(X, y, epochs, batch_size)
            
            logger.info("模型自迭代改进完成")
            return result
            
        except Exception as e:
            logger.error(f"自迭代改进失败: {e}")
            return {
                'success': False,
                'error': str(e)
            }
    
    def get_model_performance(self):
        """
        获取模型性能
        """
        return {
            'history': self.history,
            'best_score': self.best_score
        }
