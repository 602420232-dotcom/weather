"""
时间序列分析模块
提供时间序列生成、趋势分析、异常检测和预测功能
"""

import logging
logger = logging.getLogger(__name__)

from .analyzer import TimeSeriesAnalyzer

__all__ = ['TimeSeriesAnalyzer']
