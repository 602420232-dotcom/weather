"""
异常定义模块

贝叶斯数据同化核心算法库的统一异常类定义
"""


class AssimilatorError(Exception):
    """算法库基础异常"""
    pass


class DataLoadError(AssimilatorError):
    """数据加载异常"""
    pass


class ConfigurationError(AssimilatorError):
    """配置异常"""
    pass


class DataValidationError(AssimilatorError):
    """数据验证异常"""
    pass


class AlgorithmError(AssimilatorError):
    """算法执行异常"""
    pass


class ConvergenceError(AlgorithmError):
    """算法未收敛异常"""
    pass


class ParallelExecutionError(AssimilatorError):
    """并行执行异常"""
    pass


class NetworkError(AssimilatorError):
    """网络通信异常"""
    pass
