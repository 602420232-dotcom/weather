"""
贝叶斯同化系统 API
提供命令行、REST API 和 Web 界面接口
"""

import logging
logger = logging.getLogger(__name__)

from .cli import AssimilationCLI
from .rest import AssimilationAPI
from .web import create_app


__all__ = [
    'AssimilationCLI',
    'AssimilationAPI',
    'create_app'
]
