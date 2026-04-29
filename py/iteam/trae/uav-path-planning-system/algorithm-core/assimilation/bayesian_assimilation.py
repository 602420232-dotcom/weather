import numpy as np
import pandas as pd
from scipy.linalg import inv, cholesky
from typing import Dict, List, Tuple, Optional, Union
import logging

logger = logging.getLogger(__name__)

class BayesianAssimilation:
    """
    贝叶斯同化系统
    用于融合多源气象数据，生成不确定性方差场
    """
    
    def __init__(self, 
                 observation_error: float = 0.1, 
                 background_error: float = 0.2,
                 assimilation_method: str = '3dvar'):
        """
        初始化贝叶斯同化系统
        :param observation_error: 观测误差方差
        :param background_error: 背景场误差方差
        :param assimilation_method: 同化方法 ('3dvar', 'enkf', 'hybrid')
        """
        self.observation_error = observation_error
        self.background_error = background_error
        self.assimilation_method = assimilation_method
        self.assimilation_history = []
    
    def assimilate(self, 
                  background: Dict[str, np.ndarray], 
                  observations: Dict[str, np.ndarray],
                  observation_locations: Optional[Dict[str, np.ndarray]] = None) -> Tuple[Dict[str, np.ndarray], Dict[str, np.ndarray]]:
        """
        执行贝叶斯同化
        :param background: 背景场数据
        :param observations: 观测数据
        :param observation_locations: 观测位置（可选）
        :return: 同化后的分析场和不确定性方差
        """
        logger.info(f"开始贝叶斯同化，方法: {self.assimilation_method}")
        
        # 选择同化方法
        if self.assimilation_method == '3dvar':
            analysis, uncertainty = self._three_dimensional_var(background, observations)
        elif self.assimilation_method == 'enkf':
            analysis, uncertainty = self._ensemble_kalman_filter(background, observations)
        elif self.assimilation_method == 'hybrid':
            analysis, uncertainty = self._hybrid_assimilation(background, observations)
        else:
            raise ValueError(f"不支持的同化方法: {self.assimilation_method}")
        
        # 记录同化历史
        assimilation_record = {
            'timestamp': pd.Timestamp.now(),
            'method': self.assimilation_method,
            'background': background,
            'observations': observations,
            'analysis': analysis,
            'uncertainty': uncertainty
        }
        self.assimilation_history.append(assimilation_record)
        
        logger.info("贝叶斯同化完成")
        return analysis, uncertainty
    
    def _three_dimensional_var(self, 
                              background: Dict[str, np.ndarray], 
                              observations: Dict[str, np.ndarray]) -> Tuple[Dict[str, np.ndarray], Dict[str, np.ndarray]]:
        """
        3D-VAR同化方法
        :param background: 背景场
        :param observations: 观测数据
        :return: 分析场和不确定性
        """
        analysis = {}
        uncertainty = {}
        
        for var in background:
            if var in observations:
                # 确保观测数据与背景场形状一致
                if background[var].shape != observations[var].shape:
                    logger.warning(f"{var} 数据形状不一致，使用背景场")
                    analysis[var] = background[var]
                    uncertainty[var] = np.full_like(background[var], self.background_error ** 2)
                    continue
                
                # 简化的3D-VAR实现
                B = self.background_error ** 2
                R = self.observation_error ** 2
                
                # 增益矩阵
                K = B / (B + R)
                
                # 分析场
                analysis[var] = background[var] + K * (observations[var] - background[var])
                
                # 分析误差方差
                uncertainty[var] = (1 - K) * B
            else:
                analysis[var] = background[var]
                uncertainty[var] = np.full_like(background[var], self.background_error ** 2)
        
        return analysis, uncertainty
    
    def _ensemble_kalman_filter(self, 
                               background: Dict[str, np.ndarray], 
                               observations: Dict[str, np.ndarray]) -> Tuple[Dict[str, np.ndarray], Dict[str, np.ndarray]]:
        """
        集合卡尔曼滤波(EnKF)同化方法
        :param background: 背景场
        :param observations: 观测数据
        :return: 分析场和不确定性
        """
        analysis = {}
        uncertainty = {}
        
        # 简化的EnKF实现（使用单个集合成员）
        for var in background:
            if var in observations:
                # 确保观测数据与背景场形状一致
                if background[var].shape != observations[var].shape:
                    logger.warning(f"{var} 数据形状不一致，使用背景场")
                    analysis[var] = background[var]
                    uncertainty[var] = np.full_like(background[var], self.background_error ** 2)
                    continue
                
                # 计算背景场协方差
                B = np.var(background[var])
                R = self.observation_error ** 2
                
                # 增益矩阵
                K = B / (B + R)
                
                # 分析场
                analysis[var] = background[var] + K * (observations[var] - background[var])
                
                # 分析误差方差
                uncertainty[var] = (1 - K) * B
            else:
                analysis[var] = background[var]
                uncertainty[var] = np.full_like(background[var], self.background_error ** 2)
        
        return analysis, uncertainty
    
    def _hybrid_assimilation(self, 
                             background: Dict[str, np.ndarray], 
                             observations: Dict[str, np.ndarray]) -> Tuple[Dict[str, np.ndarray], Dict[str, np.ndarray]]:
        """
        混合同化方法（结合3D-VAR和EnKF）
        :param background: 背景场
        :param observations: 观测数据
        :return: 分析场和不确定性
        """
        # 先使用3D-VAR
        var_analysis, var_uncertainty = self._three_dimensional_var(background, observations)
        
        # 再使用EnKF进行调整
        enkf_analysis, enkf_uncertainty = self._ensemble_kalman_filter(var_analysis, observations)
        
        # 混合结果
        analysis = {}
        uncertainty = {}
        
        for var in background:
            # 加权平均
            analysis[var] = 0.7 * enkf_analysis[var] + 0.3 * var_analysis[var]
            uncertainty[var] = 0.7 * enkf_uncertainty[var] + 0.3 * var_uncertainty[var]
        
        return analysis, uncertainty
    
    def calculate_variance_field(self, 
                                uncertainty: Dict[str, np.ndarray], 
                                weights: Optional[Dict[str, float]] = None) -> np.ndarray:
        """
        计算综合方差场
        :param uncertainty: 各变量的不确定性
        :param weights: 各变量的权重
        :return: 综合方差场
        """
        if weights is None:
            weights = {
                'wind_speed': 0.3,
                'turbulence': 0.3,
                'thunder_risk': 0.2,
                'visibility': 0.1,
                'temperature': 0.05,
                'humidity': 0.05
            }
        
        # 初始化方差场
        var_names = list(uncertainty.keys())
        if not var_names:
            return np.array([])
        
        shape = uncertainty[var_names[0]].shape
        variance_field = np.zeros(shape)
        
        # 加权计算综合方差
        total_weight = 0
        for var, weight in weights.items():
            if var in uncertainty:
                variance_field += weight * uncertainty[var]
                total_weight += weight
        
        # 归一化
        if total_weight > 0:
            variance_field = variance_field / total_weight
        
        return variance_field
    
    def get_assimilation_history(self) -> List[Dict]:
        """
        获取同化历史记录
        :return: 同化历史列表
        """
        return self.assimilation_history
    
    def clear_history(self):
        """
        清除同化历史
        """
        self.assimilation_history = []
        logger.info("同化历史已清除")
    
    def validate_observations(self, observations: Dict[str, np.ndarray]) -> bool:
        """
        验证观测数据的有效性
        :param observations: 观测数据
        :return: 是否有效
        """
        for var, data in observations.items():
            if not isinstance(data, np.ndarray):
                logger.error(f"观测数据类型错误: {var} 不是numpy数组")
                return False
            if np.any(np.isnan(data)):
                logger.warning(f"观测数据包含NaN值: {var}")
            if np.any(np.isinf(data)):
                logger.error(f"观测数据包含无穷值: {var}")
                return False
        return True

class MultiSourceAssimilation:
    """
    多源数据同化系统
    融合雷达、地面站、无人机探空等多源数据
    """
    
    def __init__(self):
        """
        初始化多源数据同化系统
        """
        self.bayesian_assimilation = BayesianAssimilation()
        self.sources = {
            'radar': {'weight': 0.4, 'error': 0.15},
            'ground_station': {'weight': 0.3, 'error': 0.1},
            'drone_sounding': {'weight': 0.2, 'error': 0.08},
            'satellite': {'weight': 0.1, 'error': 0.2}
        }
    
    def assimilate_multi_source(self, 
                              background: Dict[str, np.ndarray],
                              multi_observations: Dict[str, Dict[str, np.ndarray]]) -> Dict[str, np.ndarray]:
        """
        多源数据同化
        :param background: 背景场
        :param multi_observations: 多源观测数据
        :return: 同化后的分析场和不确定性
        """
        logger.info(f"开始多源数据同化，源数量: {len(multi_observations)}")
        
        # 合并多源观测数据
        combined_observations = self._combine_observations(multi_observations)
        
        # 执行贝叶斯同化
        analysis, uncertainty = self.bayesian_assimilation.assimilate(background, combined_observations)
        
        # 计算综合方差场
        variance_field = self.bayesian_assimilation.calculate_variance_field(uncertainty)
        
        return {
            'analysis': analysis,
            'uncertainty': uncertainty,
            'variance_field': variance_field
        }
    
    def _combine_observations(self, 
                             multi_observations: Dict[str, Dict[str, np.ndarray]]) -> Dict[str, np.ndarray]:
        """
        合并多源观测数据
        :param multi_observations: 多源观测数据
        :return: 合并后的观测数据
        """
        combined = {}
        
        # 收集所有变量
        all_vars = set()
        for source, obs in multi_observations.items():
            all_vars.update(obs.keys())
        
        # 对每个变量进行加权平均
        for var in all_vars:
            weighted_sum = 0
            total_weight = 0
            
            for source, obs in multi_observations.items():
                if var in obs and source in self.sources:
                    weight = self.sources[source]['weight']
                    weighted_sum += weight * obs[var]
                    total_weight += weight
            
            if total_weight > 0:
                combined[var] = weighted_sum / total_weight
        
        return combined
    
    def add_data_source(self, source_name: str, weight: float, error: float):
        """
        添加数据源
        :param source_name: 数据源名称
        :param weight: 权重
        :param error: 误差
        """
        self.sources[source_name] = {'weight': weight, 'error': error}
        logger.info(f"添加数据源: {source_name}, 权重: {weight}, 误差: {error}")
    
    def remove_data_source(self, source_name: str):
        """
        移除数据源
        :param source_name: 数据源名称
        """
        if source_name in self.sources:
            del self.sources[source_name]
            logger.info(f"移除数据源: {source_name}")
    
    def get_sources(self) -> Dict[str, Dict[str, float]]:
        """
        获取所有数据源
        :return: 数据源字典
        """
        return self.sources

# 工具函数
def calculate_uncertainty(background: Dict[str, np.ndarray], 
                         observations: Dict[str, np.ndarray],
                         method: str = '3dvar') -> Dict[str, np.ndarray]:
    """
    计算不确定性
    :param background: 背景场
    :param observations: 观测数据
    :param method: 同化方法
    :return: 不确定性字典
    """
    assimilator = BayesianAssimilation(assimilation_method=method)
    _, uncertainty = assimilator.assimilate(background, observations)
    return uncertainty

def generate_variance_field(uncertainty: Dict[str, np.ndarray]) -> np.ndarray:
    """
    生成方差场
    :param uncertainty: 不确定性字典
    :return: 方差场
    """
    assimilator = BayesianAssimilation()
    return assimilator.calculate_variance_field(uncertainty)

def assimilate_wrf_with_observations(wrf_data: Dict[str, np.ndarray],
                                   observations: Dict[str, np.ndarray],
                                   method: str = 'hybrid') -> Dict[str, np.ndarray]:
    """
    同化WRF数据与观测数据
    :param wrf_data: WRF输出数据
    :param observations: 观测数据
    :param method: 同化方法
    :return: 同化结果
    """
    assimilator = BayesianAssimilation(assimilation_method=method)
    analysis, uncertainty = assimilator.assimilate(wrf_data, observations)
    variance_field = assimilator.calculate_variance_field(uncertainty)
    
    return {
        'analysis': analysis,
        'uncertainty': uncertainty,
        'variance_field': variance_field
    }

if __name__ == "__main__":
    # 示例使用
    print("贝叶斯同化系统示例")
    
    # 生成模拟数据
    def generate_mock_data(shape=(100, 100)):
        return {
            'wind_speed': np.random.rand(*shape) * 10,
            'turbulence': np.random.rand(*shape),
            'thunder_risk': np.random.rand(*shape),
            'visibility': np.random.rand(*shape) * 10000,
            'temperature': np.random.rand(*shape) * 30 - 5,
            'humidity': np.random.rand(*shape) * 100
        }
    
    # 背景场（WRF输出）
    background = generate_mock_data()
    print(f"背景场生成完成，风速范围: {np.min(background['wind_speed']):.2f} - {np.max(background['wind_speed']):.2f} m/s")
    
    # 观测数据（雷达+地面站）
    radar_obs = generate_mock_data()
    ground_obs = generate_mock_data()
    
    # 添加观测误差
    for var in radar_obs:
        radar_obs[var] += np.random.normal(0, 0.5, radar_obs[var].shape)
        ground_obs[var] += np.random.normal(0, 0.3, ground_obs[var].shape)
    
    # 单源同化
    print("\n=== 单源同化示例 ===")
    assimilator = BayesianAssimilation(assimilation_method='3dvar')
    analysis, uncertainty = assimilator.assimilate(background, radar_obs)
    print(f"3D-VAR同化完成，风速分析场范围: {np.min(analysis['wind_speed']):.2f} - {np.max(analysis['wind_speed']):.2f} m/s")
    
    # 多源同化
    print("\n=== 多源同化示例 ===")
    multi_assimilation = MultiSourceAssimilation()
    multi_observations = {
        'radar': radar_obs,
        'ground_station': ground_obs
    }
    result = multi_assimilation.assimilate_multi_source(background, multi_observations)
    
    # 计算方差场
    variance_field = result['variance_field']
    print(f"多源同化完成，方差场范围: {np.min(variance_field):.4f} - {np.max(variance_field):.4f}")
    
    # 生成风险等级
    risk_levels = np.zeros_like(variance_field)
    risk_levels[variance_field < 0.01] = 1  # 低风险
    risk_levels[(variance_field >= 0.01) & (variance_field < 0.05)] = 2  # 中等风险
    risk_levels[(variance_field >= 0.05) & (variance_field < 0.1)] = 3  # 高风险
    risk_levels[variance_field >= 0.1] = 4  # 极高风险
    
    print(f"风险等级分布: 低风险: {np.sum(risk_levels == 1)}, 中等风险: {np.sum(risk_levels == 2)}, 高风险: {np.sum(risk_levels == 3)}, 极高风险: {np.sum(risk_levels == 4)}")
    
    print("\n贝叶斯同化系统测试完成！")