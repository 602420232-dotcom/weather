import netCDF4 as nc
import numpy as np
import pandas as pd

# 解析WRF输出的NetCDF文件（低空0-1000米气象场）
def parse_wrf_nc(nc_file_path, height=100):
    """
    解析WRF输出的NetCDF文件，提取指定高度的气象要素
    :param nc_file_path: WRF输出文件路径
    :param height: 提取的高度（米）
    :return: 气象要素字典（风速、风向、湍流、能见度等）
    """
    dataset = nc.Dataset(nc_file_path)
    
    # 维度提取（lat/lon/height）
    lat = dataset.variables['lat'][:]
    lon = dataset.variables['lon'][:]
    height_levels = dataset.variables['height'][:]
    
    # 找到最接近指定高度的索引
    height_idx = np.argmin(np.abs(height_levels - height))
    
    # 提取核心气象要素
    wind_speed = dataset.variables['wind_speed'][height_idx, :, :]  # 风速 (m/s)
    wind_dir = dataset.variables['wind_dir'][height_idx, :, :]    # 风向 (°)
    turbulence = dataset.variables['turbulence'][height_idx, :, :]# 湍流强度
    visibility = dataset.variables['visibility'][height_idx, :, :]# 能见度 (m)
    thunder_risk = dataset.variables['thunder_risk'][height_idx, :, :] # 雷电风险 (0-1)
    
    # 关闭数据集
    dataset.close()
    
    return {
        'lat': lat,
        'lon': lon,
        'wind_speed': wind_speed,
        'wind_dir': wind_dir,
        'turbulence': turbulence,
        'visibility': visibility,
        'thunder_risk': thunder_risk,
        'update_time': pd.Timestamp.now()  # 数据更新时间
    }

# 气象数据标准化（用于AI模型输入）
def normalize_meteor_data(meteor_data):
    """标准化气象数据到0-1区间"""
    norm_data = {}
    for key in ['wind_speed', 'turbulence', 'thunder_risk']:
        max_val = np.max(meteor_data[key])
        min_val = np.min(meteor_data[key])
        norm_data[key] = (meteor_data[key] - min_val) / (max_val - min_val + 1e-8)
    norm_data['wind_dir'] = meteor_data['wind_dir'] / 360.0  # 风向归一化到0-1
    norm_data['visibility'] = 1 - (meteor_data['visibility'] / np.max(meteor_data['visibility']))  # 能见度反向归一化
    return norm_data