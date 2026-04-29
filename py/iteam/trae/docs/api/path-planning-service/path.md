# 路径规划服务API

路径规划服务提供无人机路径规划功能，支持VRPTW任务调度、A*全局路径规划和DWA实时避障。

## 接口列表

### 1. 执行VRPTW任务调度

**接口地址**：`POST /api/planning/vrptw`

**功能**：执行带时间窗的车辆路径问题（VRPTW）调度

**请求参数**：
- `drones`：数组，无人机信息
  - `id`：字符串，无人机ID
  - `max_payload`：数字，最大载重
  - `max_endurance`：数字，最大续航时间
  - `max_speed`：数字，最大速度
- `tasks`：数组，任务信息
  - `id`：字符串，任务ID
  - `location`：数组，任务位置 [经度, 纬度]
  - `demand`：数字，任务需求（载重）
  - `start_time`：数字，开始时间窗
  - `end_time`：数字，结束时间窗
- `weather_data`：对象，气象数据（可选）
  - `wind_speed`：数字，风速
  - `wind_direction`：数字，风向
  - `temperature`：数字，温度
  - `humidity`：数字，湿度

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "success": true,
    "routes": [
      {
        "drone_id": "drone1",
        "tasks": ["task1", "task2", "task3"],
        "total_distance": 150.5,
        "total_time": 75.25,
        "total_payload": 7.0
      },
      {
        "drone_id": "drone2",
        "tasks": ["task4", "task5"],
        "total_distance": 100.2,
        "total_time": 50.1,
        "total_payload": 6.0
      }
    ],
    "unassigned_tasks": []
  }
}
```

### 2. 执行A*全局路径规划

**接口地址**：`POST /api/planning/astar`

**功能**：执行A*全局路径规划

**请求参数**：
- `start`：数组，起始位置 [经度, 纬度]
- `goal`：数组，目标位置 [经度, 纬度]
- `weather_data`：对象，气象数据（可选）
  - `wind_speed`：数字，风速
  - `wind_direction`：数字，风向
- `obstacles`：数组，障碍物信息（可选）
  - `location`：数组，障碍物位置 [经度, 纬度]
  - `radius`：数字，障碍物半径
- `no_fly_zones`：数组，禁飞区信息（可选）
  - `location`：数组，禁飞区位置 [经度, 纬度]
  - `radius`：数字，禁飞区半径

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "success": true,
    "path": [
      [0.0, 0.0],
      [1.0, 0.5],
      [2.0, 1.0],
      [3.0, 1.5],
      [4.0, 2.0],
      [5.0, 2.5],
      [6.0, 3.0],
      [7.0, 3.5],
      [8.0, 4.0],
      [9.0, 4.5],
      [10.0, 5.0]
    ],
    "distance": 11.18
  }
}
```

### 3. 执行DWA实时避障

**接口地址**：`POST /api/planning/dwa`

**功能**：执行动态窗口法（DWA）实时避障

**请求参数**：
- `current_pose`：数组，当前位置和朝向 [经度, 纬度, 朝向]
- `goal`：数组，目标位置 [经度, 纬度]
- `weather_data`：对象，气象数据（可选）
  - `wind_speed`：数字，风速
  - `wind_direction`：数字，风向
- `obstacles`：数组，障碍物信息
  - `location`：数组，障碍物位置 [经度, 纬度]
  - `radius`：数字，障碍物半径

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "success": true,
    "trajectory": [
      [0.0, 0.0],
      [0.1, 0.05],
      [0.2, 0.1],
      [0.3, 0.15],
      [0.4, 0.2],
      [0.5, 0.25],
      [0.6, 0.3],
      [0.7, 0.35],
      [0.8, 0.4],
      [0.9, 0.45]
    ],
    "score": 9.5
  }
}
```

### 4. 执行完整路径规划

**接口地址**：`POST /api/planning/full`

**功能**：执行完整的三层路径规划（VRPTW + A* + DWA）

**请求参数**：
- `drones`：数组，无人机信息
  - `id`：字符串，无人机ID
  - `max_payload`：数字，最大载重
  - `max_endurance`：数字，最大续航时间
  - `max_speed`：数字，最大速度
- `tasks`：数组，任务信息
  - `id`：字符串，任务ID
  - `location`：数组，任务位置 [经度, 纬度]
  - `demand`：数字，任务需求（载重）
  - `start_time`：数字，开始时间窗
  - `end_time`：数字，结束时间窗
- `weather_data`：对象，气象数据（可选）
  - `wind_speed`：数字，风速
  - `wind_direction`：数字，风向
  - `temperature`：数字，温度
  - `humidity`：数字，湿度
- `obstacles`：数组，障碍物信息（可选）
  - `location`：数组，障碍物位置 [经度, 纬度]
  - `radius`：数字，障碍物半径
- `no_fly_zones`：数组，禁飞区信息（可选）
  - `location`：数组，禁飞区位置 [经度, 纬度]
  - `radius`：数字，禁飞区半径

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "success": true,
    "routes": [
      {
        "drone_id": "drone1",
        "tasks": ["task1", "task2", "task3"],
        "total_distance": 150.5,
        "total_time": 75.25,
        "total_payload": 7.0,
        "path": [
          [0.0, 0.0],
          [1.0, 0.5],
          [2.0, 1.0],
          [3.0, 1.5],
          [4.0, 2.0],
          [5.0, 2.5],
          [6.0, 3.0],
          [7.0, 3.5],
          [8.0, 4.0],
          [9.0, 4.5],
          [10.0, 5.0],
          [0.0, 0.0]
        ]
      }
    ],
    "unassigned_tasks": []
  }
}
```

### 5. 执行RRT*路径规划

**接口地址**：`POST /api/planning/rrt`

**功能**：执行RRT*路径规划算法

**请求参数**：
- `start`：数组，起始位置 [经度, 纬度]
- `goal`：数组，目标位置 [经度, 纬度]
- `obstacles`：数组，障碍物信息
  - `location`：数组，障碍物位置 [经度, 纬度]
  - `radius`：数字，障碍物半径
- `params`：对象，算法参数（可选）
  - `max_iterations`：数字，最大迭代次数
  - `step_size`：数字，步长
  - `goal_bias`：数字，目标偏置

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "success": true,
    "path": [
      [0.0, 0.0],
      [1.0, 0.2],
      [2.0, 0.5],
      [3.0, 0.8],
      [4.0, 1.2],
      [5.0, 1.5],
      [6.0, 1.8],
      [7.0, 2.2],
      [8.0, 2.5],
      [9.0, 2.8],
      [10.0, 3.0]
    ],
    "distance": 10.44
  }
}
```

## 错误处理

| 错误代码 | 错误信息 | 描述 |
|---------|---------|------|
| 400 | 请求参数错误 | 缺少必要参数或参数格式不正确 |
| 401 | 未授权 | 缺少或无效的认证令牌 |
| 500 | 服务器内部错误 | 处理过程中发生错误 |

## 示例代码

### Python示例

```python
import requests
import json

# 执行VRPTW任务调度
url = "http://localhost:8083/api/planning/vrptw"
payload = {
    "drones": [
        {
            "id": "drone1",
            "max_payload": 10.0,
            "max_endurance": 120.0,
            "max_speed": 20.0
        },
        {
            "id": "drone2",
            "max_payload": 15.0,
            "max_endurance": 150.0,
            "max_speed": 25.0
        }
    ],
    "tasks": [
        {
            "id": "task1",
            "location": [10.0, 10.0],
            "demand": 2.0,
            "start_time": 0.0,
            "end_time": 60.0
        },
        {
            "id": "task2",
            "location": [20.0, 20.0],
            "demand": 3.0,
            "start_time": 30.0,
            "end_time": 90.0
        },
        {
            "id": "task3",
            "location": [30.0, 30.0],
            "demand": 2.0,
            "start_time": 60.0,
            "end_time": 120.0
        }
    ],
    "weather_data": {
        "wind_speed": 5.0,
        "wind_direction": 180,
        "temperature": 20.0,
        "humidity": 60
    }
}

response = requests.post(url, json=payload)
print(response.json())

# 执行A*全局路径规划
url = "http://localhost:8083/api/planning/astar"
payload = {
    "start": [0.0, 0.0],
    "goal": [10.0, 10.0],
    "obstacles": [
        {
            "location": [5.0, 5.0],
            "radius": 2.0
        }
    ],
    "no_fly_zones": [
        {
            "location": [7.0, 7.0],
            "radius": 1.5
        }
    ]
}

response = requests.post(url, json=payload)
print(response.json())
```
