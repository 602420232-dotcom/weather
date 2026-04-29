"""
日志工具模块
提供日志配置和管理功能
"""

import logging
from typing import Optional, Dict, Any


def setup_logging(level: int = logging.INFO, 
                 format_str: Optional[str] = None,
                 log_file: Optional[str] = None) -> logging.Logger:
    """
    设置日志配置
    
    Args:
        level: 日志级别
        format_str: 日志格式字符串
        log_file: 日志文件路径（可选）
        
    Returns:
        配置好的根日志记录器
    """
    if format_str is None:
        format_str = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    
    # 创建日志记录器
    logger = logging.getLogger()
    logger.setLevel(level)
    
    # 清除已有的处理器
    logger.handlers.clear()
    
    # 创建格式化器
    formatter = logging.Formatter(format_str)
    
    # 添加控制台处理器
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)
    
    # 如果指定了日志文件，添加文件处理器
    if log_file:
        file_handler = logging.FileHandler(log_file, encoding='utf-8')
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)
    
    return logger


def get_logger(name: str) -> logging.Logger:
    """
    获取指定名称的日志记录器
    
    Args:
        name: 日志记录器名称
        
    Returns:
        日志记录器实例
    """
    return logging.getLogger(name)


def log_function_call(func):
    """
    装饰器：记录函数调用
    
    Args:
        func: 被装饰的函数
        
    Returns:
        装饰后的函数
    """
    def wrapper(*args, **kwargs):
        logger = logging.getLogger(func.__module__)
        logger.debug(f"调用函数: {func.__name__}")
        try:
            result = func(*args, **kwargs)
            logger.debug(f"函数 {func.__name__} 调用成功")
            return result
        except Exception as e:
            logger.error(f"函数 {func.__name__} 调用失败: {e}")
            raise
    return wrapper
