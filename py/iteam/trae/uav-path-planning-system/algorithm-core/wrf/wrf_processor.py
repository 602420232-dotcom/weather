import netCDF4 as nc
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from datetime import datetime, timedelta
import json

class WRFProcessor:
    """
    WRF气象数据处理器
    负责解析、预处理、质量控制和提供气象数据接口
    """
    
    def __init__(self, nc_file_path):
        """
        初始化WRF处理器
        :param nc_file_path: WRF输出文件路径
        """
        self.nc_file_path = nc_file_path
        self.dataset = None
        self.lat = None
        self.lon = None
        self.height_levels = None
        self.time = None
        self._open_dataset()
        self._extract_dimensions()
    
    def _open_dataset(self):
        """打开NetCDF数据集"""
        try:
            self.dataset = nc.Dataset(self.nc_file_path)
            print(f"成功打开WRF文件: {self.nc_file_path}")
        except Exception as e:
            print(f"打开WRF文件失败: {e}")
            raise
    
    def _extract_dimensions(self):
        """提取数据集维度信息"""
        if self.dataset:
            self.lat = self.dataset.variables['lat'][:]
            self.lon = self.dataset.variables['lon'][:]
            self.height_levels = self.dataset.variables.get('height', None)
            if self.height_levels is not None:
                self.height_levels = self.height_levels[:]
            # 提取时间变量
            if 'time' in self.dataset.variables:
                time_var = self.dataset.variables['time']
                self.time = nc.num2date(time_var[:], time_var.units)
    
    def get_meteorological_data(self, height=100, time_idx=0):
        """
        获取指定高度和时间的气象数据
        :param height: 高度（米）
        :param time_idx: 时间索引
        :return: 气象数据字典
        """
        if not self.dataset:
            raise ValueError("数据集未打开")
        
        # 找到最接近指定高度的索引
        height_idx = self._get_height_index(height)
        
        # 提取核心气象要素
        meteor_data = {
            'lat': self.lat,
            'lon': self.lon,
            'height': height,
            'time': self.time[time_idx] if self.time is not None else datetime.now(),
            'wind_speed': self._extract_variable('wind_speed', height_idx, time_idx),
            'wind_dir': self._extract_variable('wind_dir', height_idx, time_idx),
            'turbulence': self._extract_variable('turbulence', height_idx, time_idx),
            'visibility': self._extract_variable('visibility', height_idx, time_idx),
            'thunder_risk': self._extract_variable('thunder_risk', height_idx, time_idx),
            'temperature': self._extract_variable('temperature', height_idx, time_idx),
            'humidity': self._extract_variable('humidity', height_idx, time_idx)
        }
        
        # 数据质量控制
        meteor_data = self._quality_control(meteor_data)
        
        return meteor_data
    
    def _get_height_index(self, target_height):
        """找到最接近目标高度的索引"""
        if self.height_levels is None:
            return 0  # 默认使用第一个高度层
        return np.argmin(np.abs(self.height_levels - target_height))
    
    def _extract_variable(self, var_name, height_idx, time_idx):
        """提取变量数据"""
        if var_name in self.dataset.variables:
            var = self.dataset.variables[var_name]
            # 根据变量维度提取数据
            if len(var.shape) == 4:  # (time, height, lat, lon)
                return var[time_idx, height_idx, :, :]
            elif len(var.shape) == 3:  # (height, lat, lon)
                return var[height_idx, :, :]
            elif len(var.shape) == 2:  # (lat, lon)
                return var[:, :]
        return np.zeros_like(self.lat)
    
    def _quality_control(self, data):
        """数据质量控制"""
        # 处理无效值
        for key in data:
            if isinstance(data[key], np.ndarray):
                # 将负值和异常值设置为合理范围
                if key in ['wind_speed', 'visibility']:
                    data[key] = np.maximum(data[key], 0)
                elif key in ['turbulence', 'thunder_risk']:
                    data[key] = np.clip(data[key], 0, 1)
                elif key in ['temperature']:
                    data[key] = np.clip(data[key], -50, 50)
                elif key in ['humidity']:
                    data[key] = np.clip(data[key], 0, 100)
        return data
    
    def normalize_data(self, meteor_data):
        """
        标准化气象数据到0-1区间
        :param meteor_data: 气象数据字典
        :return: 标准化后的数据
        """
        norm_data = {}
        for key in meteor_data:
            if isinstance(meteor_data[key], np.ndarray):
                if key in ['wind_speed', 'turbulence', 'thunder_risk']:
                    max_val = np.max(meteor_data[key])
                    min_val = np.min(meteor_data[key])
                    norm_data[key] = (meteor_data[key] - min_val) / (max_val - min_val + 1e-8)
                elif key == 'wind_dir':
                    norm_data[key] = meteor_data[key] / 360.0
                elif key == 'visibility':
                    max_vis = np.max(meteor_data[key])
                    norm_data[key] = 1 - (meteor_data[key] / max_vis)
                elif key == 'temperature':
                    norm_data[key] = (meteor_data[key] + 50) / 100.0
                elif key == 'humidity':
                    norm_data[key] = meteor_data[key] / 100.0
            else:
                norm_data[key] = meteor_data[key]
        return norm_data
    
    def get_time_series(self, lat_idx, lon_idx, height=100):
        """
        获取指定位置的时间序列数据
        :param lat_idx: 纬度索引
        :param lon_idx: 经度索引
        :param height: 高度
        :return: 时间序列数据
        """
        if not self.dataset or self.time is None:
            return None
        
        height_idx = self._get_height_index(height)
        time_series = []
        
        for t_idx in range(len(self.time)):
            data = self.get_meteorological_data(height, t_idx)
            point_data = {
                'time': self.time[t_idx],
                'wind_speed': data['wind_speed'][lat_idx, lon_idx],
                'wind_dir': data['wind_dir'][lat_idx, lon_idx],
                'turbulence': data['turbulence'][lat_idx, lon_idx],
                'visibility': data['visibility'][lat_idx, lon_idx],
                'thunder_risk': data['thunder_risk'][lat_idx, lon_idx],
                'temperature': data['temperature'][lat_idx, lon_idx],
                'humidity': data['humidity'][lat_idx, lon_idx]
            }
            time_series.append(point_data)
        
        return time_series
    
    def visualize_data(self, variable, height=100, time_idx=0, save_path=None):
        """
        可视化气象数据
        :param variable: 变量名
        :param height: 高度
        :param time_idx: 时间索引
        :param save_path: 保存路径
        """
        data = self.get_meteorological_data(height, time_idx)
        if variable not in data:
            print(f"变量 {variable} 不存在")
            return
        
        plt.figure(figsize=(10, 8))
        plt.contourf(data['lon'], data['lat'], data[variable], cmap='viridis')
        plt.colorbar(label=variable)
        plt.title(f'{variable} at {height}m, {data["time"]}')
        plt.xlabel('Longitude')
        plt.ylabel('Latitude')
        
        if save_path:
            plt.savefig(save_path)
            print(f"图像保存到: {save_path}")
        else:
            plt.show()
    
    def to_json(self, data):
        """
        将气象数据转换为JSON格式
        :param data: 气象数据字典
        :return: JSON字符串
        """
        # 处理numpy数组和时间对象
        def convert_to_serializable(obj):
            if isinstance(obj, np.ndarray):
                return obj.tolist()
            elif isinstance(obj, datetime):
                return obj.isoformat()
            elif isinstance(obj, np.generic):
                return obj.item()
            return obj
        
        # 递归转换
        def recursive_convert(data):
            if isinstance(data, dict):
                return {k: recursive_convert(v) for k, v in data.items()}
            elif isinstance(data, list):
                return [recursive_convert(item) for item in data]
            else:
                return convert_to_serializable(data)
        
        return json.dumps(recursive_convert(data), indent=2)
    
    def close(self):
        """关闭数据集"""
        if self.dataset:
            self.dataset.close()
            self.dataset = None
            print("数据集已关闭")
    
    def __enter__(self):
        """上下文管理器进入"""
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """上下文管理器退出"""
        self.close()

# 工具函数
def parse_wrf_nc(nc_file_path, height=100, time_idx=0):
    """
    解析WRF输出的NetCDF文件
    :param nc_file_path: WRF输出文件路径
    :param height: 提取的高度（米）
    :param time_idx: 时间索引
    :return: 气象要素字典
    """
    with WRFProcessor(nc_file_path) as processor:
        return processor.get_meteorological_data(height, time_idx)

def normalize_meteor_data(meteor_data):
    """
    标准化气象数据
    :param meteor_data: 气象数据字典
    :return: 标准化后的数据
    """
    processor = WRFProcessor('dummy')  # 临时实例
    normalized = processor.normalize_data(meteor_data)
    processor.close()
    return normalized

def get_meteorological_risk(meteor_data):
    """
    计算气象风险场
    :param meteor_data: 气象数据字典
    :return: 风险场（0-1）
    """
    # 风险因子权重
    weights = {
        'wind_speed': 0.3,
        'turbulence': 0.4,
        'thunder_risk': 0.2,
        'visibility': 0.1
    }
    
    # 标准化各风险因子
    norm_data = normalize_meteor_data(meteor_data)
    
    # 计算综合风险
    risk_field = np.zeros_like(norm_data['wind_speed'])
    for factor, weight in weights.items():
        if factor in norm_data:
            risk_field += norm_data[factor] * weight
    
    # 归一化到0-1
    max_risk = np.max(risk_field)
    if max_risk > 0:
        risk_field = risk_field / max_risk
    
    return risk_field

if __name__ == "__main__":
    # 示例使用
    wrf_file = "wrf_output.nc"  # 替换为实际文件路径
    
    try:
        # 方法1：使用上下文管理器
        with WRFProcessor(wrf_file) as processor:
            # 获取气象数据
            data = processor.get_meteorological_data(height=100, time_idx=0)
            print(f"获取数据成功，风速范围: {np.min(data['wind_speed']):.2f} - {np.max(data['wind_speed']):.2f} m/s")
            
            # 标准化数据
            norm_data = processor.normalize_data(data)
            print(f"标准化后风速范围: {np.min(norm_data['wind_speed']):.2f} - {np.max(norm_data['wind_speed']):.2f}")
            
            # 计算风险场
            risk_field = get_meteorological_risk(data)
            print(f"风险场范围: {np.min(risk_field):.2f} - {np.max(risk_field):.2f}")
            
            # 可视化
            # processor.visualize_data('wind_speed', height=100, time_idx=0)
            # processor.visualize_data('turbulence', height=100, time_idx=0)
            # processor.visualize_data('thunder_risk', height=100, time_idx=0)
            
            # 获取时间序列
            # time_series = processor.get_time_series(50, 50, height=100)
            # print(f"时间序列长度: {len(time_series) if time_series else 0}")
            
    except Exception as e:
        print(f"处理WRF数据时出错: {e}")
        print("使用模拟数据进行演示...")
        
        # 模拟数据演示
        mock_data = {
            'lat': np.linspace(39.0, 40.0, 100),
            'lon': np.linspace(116.0, 117.0, 100),
            'wind_speed': np.random.rand(100, 100) * 10,
            'turbulence': np.random.rand(100, 100),
            'thunder_risk': np.random.rand(100, 100),
            'visibility': np.random.rand(100, 100) * 10000,
            'temperature': np.random.rand(100, 100) * 30 - 5,
            'humidity': np.random.rand(100, 100) * 100
        }
        
        norm_data = normalize_meteor_data(mock_data)
        risk_field = get_meteorological_risk(mock_data)
        print(f"模拟数据 - 风险场范围: {np.min(risk_field):.2f} - {np.max(risk_field):.2f}")
        print("WRF处理器初始化成功，功能演示完成！")