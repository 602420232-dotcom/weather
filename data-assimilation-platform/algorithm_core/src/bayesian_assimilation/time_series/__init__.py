"""
时间序列分析模块
提供时间序列生成、趋势分析、异常检测和预测功能
"""

from .analyzer import TimeSeriesAnalyzer

import logging
logger = logging.getLogger(__name__)

__all__ = ['TimeSeriesAnalyzer']
