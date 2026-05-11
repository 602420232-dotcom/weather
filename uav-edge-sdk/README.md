# UAV Edge SDK

无人机边缘计?SDK提供高性能的路径规划和气象风险评估功能?

## 特?

- **高性能 C++ 核心**使?A* 算法实现快速路径规?
- **气象风险评估**多因素综合评估飞行风险
- **飞控接口**支?MAVLink 协议通信
- **Python 封装**易于集成到 Python 项目
- **自动降级**C++ 模块不可用时自动切换到纯 Python 实现

## 安装

### 从源码编排

```bash
# 克隆仓库
git clone https://github.com/your-repo/uav-edge-sdk.git
cd uav-edge-sdk

# 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Linux/Mac
# venv\Scripts\activate  # Windows

# 安装依赖
pip install -r requirements.txt

# 编译 C++ 模块
./build.sh  # Linux/Mac
# build.bat  # Windows

# 安装 SDK
pip install -e .
```

### 使用预编译轮?

```bash
pip install edge-sdk
```

## 快速开?

### 基本用法

```python
from edge_sdk import create_sdk

# 创建 SDK 实例
sdk = create_sdk({
    'grid_width': 100,    # 网格宽度米?
    'grid_height': 100,   # 网格高度米?
    'resolution': 1.0      # 分辨率?格
})

# 路径规划
path = sdk.plan_path(
    start=(0, 0),
    goal=(50, 50),
    obstacles=[(10, 10), (10, 11), (11, 10)]
)
print(f"路径点数: {len(path)}")

# 气象风险评估
weather = {
    'wind_speed': 8.0,        # 风?m/s
    'wind_direction': 180,     # 风向 ?
    'temperature': 25.0,       # 温度 C
    'humidity': 65.0,          # 湿度 %
    'visibility': 10.0,         # 能见?km
    'precipitation': 0.0,      # 降水?mm/h
    'has_thunderstorm': False  # 是否有雷?
}
result = sdk.assess_weather_risk(weather)
print(f"风险等级: {result['level']}")
print(f"风险分数: {result['score']}")
```

### 便捷函数

```python
from edge_sdk import plan_path, assess_weather

# 快速路径规?
path = plan_path((0, 0), (10, 10))

# 快速气象评?
weather = {'wind_speed': 5.0, 'wind_direction': 180, ...}
result = assess_weather(weather)
```

### 飞控接口

```python
sdk = create_sdk({'serial_device': '/dev/ttyUSB0', 'baudrate': 57600})

# 连接飞控
if sdk.connect_flight_controller():
    # 获取无人机状态
    state = sdk.get_uav_state()
    print(f"位置: {state['latitude']}, {state['longitude']}")
    print(f"高度: {state['altitude']}m")
    print(f"电量: {state['battery']}%")
    
    # 上传任务
    waypoints = [
        {'latitude': 30.0, 'longitude': 120.0, 'altitude': 100},
        {'latitude': 30.1, 'longitude': 120.1, 'altitude': 100},
    ]
    sdk.upload_mission(waypoints)
    
    # 执行任务
    sdk.execute_mission()
```

## API 参?

### EdgeSDK ?

#### 构造函?

```python
sdk = EdgeSDK(config=None)
```

- `config`: 配置字典可选键?
  - `grid_width`: 网格宽度默?100
  - `grid_height`: 网格高度默?100
  - `resolution`: 分辨率默认 1.0
  - `serial_device`: 串口设备路径
  - `baudrate`: 串口波特?

#### 路径规划

```python
path = sdk.plan_path(start, goal, obstacles=None)
```

- `start`: 起点坐标 `(x, y)`
- `goal`: 终点坐标 `(x, y)`
- `obstacles`: 障碍物列表`[(x1, y1), (x2, y2), ...]`
- 返回: 路径点列表`[(x1, y1), (x2, y2), ...]`

#### 气象风险评估

```python
result = sdk.assess_weather_risk(weather)
```

- `weather`: 气象数据字典
  - `wind_speed`: 风?(m/s)
  - `wind_direction`: 风向 (?
  - `temperature`: 温度 (C)
  - `humidity`: 湿度 (%)
  - `visibility`: 能见?(km)
  - `precipitation`: 降水?(mm/h)
  - `has_thunderstorm`: 是否有雷?
- 返回: 评估结果
  - `level`: 风险等级 `'LOW'`, `'MEDIUM'`, `'HIGH'`, `'SEVERE'`
  - `score`: 风险分数 (0-100)
  - `warnings`: 警告信息列表

## 项目结构

```
uav-edge-sdk/
 edge_sdk/              # Python ?
-   __init__.py
-   _core.py          # 核心封装
-   config.py         # 配置
-   logger.py          # 日志
-   path_planner_python.py    # Python 路径规划回退
-   risk_assessor_python.py   # Python 风险评估回退
 include/               # C++ 头文?
-   path_planner.h
-   risk_assessor.h
-   flight_controller.h
 src/                   # C++ 源文?
-   path_planner.cpp
-   risk_assessor.cpp
-   flight_controller.cpp
-   bindings.cpp       # Python 绑定
 tests/                 # 测试
 CMakeLists.txt         # CMake 构建配置
 build.sh               # Linux/Mac 构建脚本
 build.bat              # Windows 构建脚本
 setup.py               # Python 安装配置
 README.md
```

## 构建说明

### Linux/Mac

```bash
./build.sh
```

### Windows

```cmd
build.bat
```

### 手动构建

```bash
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

## 性能对比

| 功能 | C++ 模块 | Python 回退 | 加速比 |
|------|----------|-------------|--------|
| 路径规划 (100x100 网格) | ~1ms | ~50ms | 50x |
| 风险评估 | ~0.1ms | ~1ms | 10x |

## 依赖

### Python 依赖

- Python >= 3.8
- numpy (可选用于高性能计算)

### C++ 依赖

- CMake >= 3.10
- C++14 兼容编译?
- pybind11 (自动下载)
- Python Development (Python.h)

## 许可选

Apache License 2.0

## 作?

Dithiothreitol

## 贡献

欢迎提交 Issue ?Pull Request?

