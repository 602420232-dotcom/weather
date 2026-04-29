# WRF处理服务API

WRF处理服务提供气象数据的解析和处理功能，支持从WRF输出文件中提取低空气象参数。

## 接口列表

### 1. 解析WRF文件

**接口地址**：`POST /api/wrf/parse`

**功能**：解析WRF输出的NetCDF4文件，提取气象数据

**请求参数**：
- `file`：文件，WRF输出的NetCDF4文件
- `options`：对象，解析选项
  - `level`：字符串，垂直层次（如 "surface"、"low"、"mid"、"high"）
  - `variables`：数组，需要提取的变量列表
  - `time_range`：对象，时间范围
    - `start`：字符串，开始时间（ISO格式）
    - `end`：字符串，结束时间（ISO格式）

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "file_id": "wrf_20260415_1200",
    "variables": ["temperature", "humidity", "wind_speed", "wind_direction"],
    "time_steps": 24,
    "levels": ["surface", "low"]
  }
}
```

### 2. 获取处理后的气象数据

**接口地址**：`GET /api/wrf/data`

**功能**：获取解析后的气象数据

**请求参数**：
- `file_id`：字符串，文件ID
- `variable`：字符串，变量名
- `level`：字符串，垂直层次
- `time`：字符串，时间（ISO格式）
- `bbox`：对象，地理范围
  - `min_lon`：数字，最小经度
  - `min_lat`：数字，最小纬度
  - `max_lon`：数字，最大经度
  - `max_lat`：数字，最大纬度

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "variable": "temperature",
    "level": "surface",
    "time": "2026-04-15T12:00:00Z",
    "grid": {
      "lon": [116.0, 116.1, 116.2],
      "lat": [39.0, 39.1, 39.2],
      "values": [[20.5, 20.6, 20.7], [20.8, 20.9, 21.0], [21.1, 21.2, 21.3]]
    }
  }
}
```

### 3. 获取数据统计信息

**接口地址**：`GET /api/wrf/stats`

**功能**：获取气象数据的统计信息

**请求参数**：
- `file_id`：字符串，文件ID
- `variable`：字符串，变量名
- `level`：字符串，垂直层次
- `time_range`：对象，时间范围
  - `start`：字符串，开始时间（ISO格式）
  - `end`：字符串，结束时间（ISO格式）

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "variable": "temperature",
    "level": "surface",
    "statistics": {
      "min": 15.2,
      "max": 25.8,
      "mean": 20.5,
      "std": 2.3
    },
    "time_series": {
      "times": ["2026-04-15T12:00:00Z", "2026-04-15T13:00:00Z"],
      "values": [20.5, 21.0]
    }
  }
}
```

### 4. 获取垂直剖面数据

**接口地址**：`GET /api/wrf/profile`

**功能**：获取垂直剖面的气象数据

**请求参数**：
- `file_id`：字符串，文件ID
- `variable`：字符串，变量名
- `time`：字符串，时间（ISO格式）
- `point`：对象，地理点
  - `lon`：数字，经度
  - `lat`：数字，纬度

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "variable": "temperature",
    "time": "2026-04-15T12:00:00Z",
    "point": {"lon": 116.3, "lat": 39.9},
    "profile": {
      "levels": [1000, 950, 900, 850, 800, 700, 600, 500],
      "values": [20.5, 19.8, 18.5, 17.2, 15.8, 12.5, 8.2, 5.0]
    }
  }
}
```

### 5. 批量处理WRF文件

**接口地址**：`POST /api/wrf/batch`

**功能**：批量处理多个WRF文件

**请求参数**：
- `files`：数组，文件列表
- `options`：对象，解析选项
  - `level`：字符串，垂直层次
  - `variables`：数组，需要提取的变量列表

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "processed_files": 5,
    "file_ids": ["wrf_20260415_1200", "wrf_20260415_1800", "wrf_20260416_0000", "wrf_20260416_0600", "wrf_20260416_1200"]
  }
}
```

## 错误处理

| 错误代码 | 错误信息 | 描述 |
|---------|---------|------|
| 400 | 请求参数错误 | 缺少必要参数或参数格式不正确 |
| 401 | 未授权 | 缺少或无效的认证令牌 |
| 404 | 文件不存在 | 指定的file_id不存在 |
| 500 | 服务器内部错误 | 处理过程中发生错误 |

## 示例代码

### Python示例

```python
import requests
import json

# 解析WRF文件
url = "http://localhost:8081/api/wrf/parse"
files = {'file': open('wrfout_d01_2026-04-15_12:00:00', 'rb')}
options = {
    'level': 'surface',
    'variables': ['temperature', 'humidity', 'wind_speed'],
    'time_range': {
        'start': '2026-04-15T12:00:00Z',
        'end': '2026-04-16T12:00:00Z'
    }
}

response = requests.post(url, files=files, data={'options': json.dumps(options)})
print(response.json())

# 获取气象数据
url = "http://localhost:8081/api/wrf/data"
params = {
    'file_id': 'wrf_20260415_1200',
    'variable': 'temperature',
    'level': 'surface',
    'time': '2026-04-15T12:00:00Z',
    'bbox': {
        'min_lon': 116.0,
        'min_lat': 39.0,
        'max_lon': 117.0,
        'max_lat': 40.0
    }
}

response = requests.get(url, params=params)
print(response.json())
```
