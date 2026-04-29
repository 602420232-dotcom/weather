# data_sources/buoy.py
# 浮标数据源处理

import numpy as np
from typing import Optional, List, Dict, Any, Tuple
import csv
import json
import logging

from .base import DataSourceBase

logger = logging.getLogger(__name__)


class BuoyDataSource(DataSourceBase):
    """
    浮标数据源
    处理海洋浮标观测数据
    """
    
    def __init__(self, config: Optional[Dict[str, Any]] = None):
        super().__init__(config)
        self.buoy_type = self.config.get('buoy_type', 'oceanographic')
        self.data_format = self.config.get('data_format', 'csv')
    
    def load_data(self, file_path: str, *args, **kwargs) -> bool:
        """
        加载浮标数据
        
        Args:
            file_path: 数据文件路径
        
        Returns:
            bool: 加载是否成功
        """
        try:
            if self.data_format == 'csv':
                self._load_csv(file_path)
            elif self.data_format == 'json':
                self._load_json(file_path)
            else:
                logger.error(f"不支持的数据格式: {self.data_format}")
                return False
            
            logger.info(f"成功加载浮标数据: {file_path}")
            return True
        except Exception as e:
            logger.error(f"加载浮标数据失败: {e}")
            return False
    
    def _load_csv(self, file_path: str):
        """
        加载CSV格式的浮标数据
        """
        self.data = []
        with open(file_path, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                # 转换数据类型
                processed_row = {}
                for key, value in row.items():
                    try:
                        processed_row[key] = float(value)
                    except ValueError:
                        processed_row[key] = value
                self.data.append(processed_row)
        
        # 提取元数据
        if self.data:
            self.metadata['buoy_id'] = self.data[0].get('buoy_id', 'unknown')
            self.metadata['buoy_name'] = self.data[0].get('buoy_name', 'unknown')
            self.metadata['data_format'] = 'csv'
    
    def _load_json(self, file_path: str):
        """
        加载JSON格式的浮标数据
        """
        with open(file_path, 'r', encoding='utf-8') as f:
            self.data = json.load(f)
        
        # 提取元数据
        if isinstance(self.data, dict):
            self.metadata['buoy_id'] = self.data.get('buoy_id', 'unknown')
            self.metadata['buoy_name'] = self.data.get('buoy_name', 'unknown')
            self.metadata['data_format'] = 'json'
        elif isinstance(self.data, list) and self.data:
            self.metadata['buoy_id'] = self.data[0].get('buoy_id', 'unknown')
            self.metadata['buoy_name'] = self.data[0].get('buoy_name', 'unknown')
            self.metadata['data_format'] = 'json'
    
    def process_data(self) -> Any:
        """
        处理浮标数据
        
        Returns:
            Any: 处理后的数据
        """
        if not self.validate_data():
            return None
        
        try:
            # 处理不同类型的浮标数据
            if self.buoy_type == 'oceanographic':
                return self._process_oceanographic_data()
            elif self.buoy_type == 'meteorological':
                return self._process_meteorological_data()
            else:
                logger.warning("未知的浮标类型")
                return self.data
        except Exception as e:
            logger.error(f"处理浮标数据失败: {e}")
            return None
    
    def _process_oceanographic_data(self) -> Dict[str, Any]:
        """
        处理海洋学浮标数据
        """
        # 处理海洋学数据，只保留与路径规划相关的参数
        processed_data = {
            'air_temperature': [],
            'humidity': [],
            'barometric_pressure': [],
            'wind_speed': [],
            'wind_direction': [],
            'visibility': [],
            'timestamp': []
        }
        
        for record in self.data:
            if isinstance(record, dict):
                processed_data['air_temperature'].append(record.get('air_temperature', record.get('temperature', 0.0)))
                processed_data['humidity'].append(record.get('humidity', 0.0))
                processed_data['barometric_pressure'].append(record.get('barometric_pressure', record.get('pressure', 0.0)))
                processed_data['wind_speed'].append(record.get('wind_speed', 0.0))
                processed_data['wind_direction'].append(record.get('wind_direction', 0.0))
                processed_data['visibility'].append(record.get('visibility', 0.0))
                processed_data['timestamp'].append(record.get('timestamp', ''))
        
        return processed_data
    
    def _process_meteorological_data(self) -> Dict[str, Any]:
        """
        处理气象浮标数据
        """
        # 处理气象数据
        processed_data = {
            'air_temperature': [],
            'humidity': [],
            'barometric_pressure': [],
            'wind_speed': [],
            'wind_direction': [],
            'visibility': [],
            'timestamp': []
        }
        
        for record in self.data:
            if isinstance(record, dict):
                processed_data['air_temperature'].append(record.get('air_temperature', 0.0))
                processed_data['humidity'].append(record.get('humidity', 0.0))
                processed_data['barometric_pressure'].append(record.get('barometric_pressure', 0.0))
                processed_data['wind_speed'].append(record.get('wind_speed', 0.0))
                processed_data['wind_direction'].append(record.get('wind_direction', 0.0))
                processed_data['visibility'].append(record.get('visibility', 0.0))
                processed_data['timestamp'].append(record.get('timestamp', ''))
        
        return processed_data
    
    def get_observations(self) -> Tuple[List[float], List[Tuple[float, float, float]], List[float]]:
        """
        获取观测数据
        
        Returns:
            Tuple[观测值列表, 观测位置列表, 观测误差列表]
        """
        if not self.validate_data():
            return [], [], []
        
        try:
            obs_values = []
            obs_locations = []
            obs_errors = []
            
            for record in self.data:
                if isinstance(record, dict):
                    # 提取观测值，只处理与路径规划相关的参数
                    if 'air_temperature' in record:
                        obs_values.append(record['air_temperature'])
                    elif 'temperature' in record:
                        obs_values.append(record['temperature'])
                    elif 'wind_speed' in record:
                        obs_values.append(record['wind_speed'])
                    elif 'humidity' in record:
                        obs_values.append(record['humidity'])
                    elif 'pressure' in record:
                        obs_values.append(record['pressure'])
                    elif 'visibility' in record:
                        obs_values.append(record['visibility'])
                    else:
                        continue
                    
                    # 提取观测位置
                    lat = record.get('latitude', 0.0)
                    lon = record.get('longitude', 0.0)
                    alt = record.get('altitude', 0.0)
                    obs_locations.append((lon, lat, alt))
                    
                    # 生成观测误差
                    obs_errors.append(0.1)  # 默认误差
            
            return obs_values, obs_locations, obs_errors
        except Exception as e:
            logger.error(f"获取观测数据失败: {e}")
            return [], [], []
