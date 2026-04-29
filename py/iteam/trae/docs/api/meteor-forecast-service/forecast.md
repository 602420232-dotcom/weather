# 气象预测服务API

气象预测服务提供气象数据的预测和订正功能，使用LSTM和XGBoost模型进行气象预测。

## 接口列表

### 1. 执行气象预测

**接口地址**：`POST /api/forecast/predict`

**功能**：使用LSTM模型执行气象预测

**请求参数**：
- `model_id`：字符串，模型ID
- `input_data`：对象，输入数据
  - `variables`：对象，气象变量数据
    - `temperature`：数组，温度历史数据
    - `humidity`：数组，湿度历史数据
    - `wind_speed`：数组，风速历史数据
    - `wind_direction`：数组，风向历史数据
  - `time_steps`：数字，输入时间步数
  - `prediction_steps`：数字，预测时间步数

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "model_id": "lstm_model_001",
    "predictions": {
      "temperature": [20.5, 21.0, 21.5, 22.0, 22.5],
      "humidity": [60, 58, 56, 54, 52],
      "wind_speed": [5.0, 5.2, 5.5, 5.8, 6.0],
      "wind_direction": [180, 185, 190, 195, 200]
    },
    "timestamps": ["2026-04-15T12:00:00Z", "2026-04-15T13:00:00Z", "2026-04-15T14:00:00Z", "2026-04-15T15:00:00Z", "2026-04-15T16:00:00Z"]
  }
}
```

### 2. 执行气象数据订正

**接口地址**：`POST /api/forecast/correct`

**功能**：使用XGBoost模型执行气象数据订正

**请求参数**：
- `model_id`：字符串，模型ID
- `forecast_data`：对象，预测数据
  - `temperature`：数组，温度预测数据
  - `humidity`：数组，湿度预测数据
  - `wind_speed`：数组，风速预测数据
  - `wind_direction`：数组，风向预测数据
- `observation_data`：对象，观测数据（可选）
  - `temperature`：数组，温度观测数据
  - `humidity`：数组，湿度观测数据
  - `wind_speed`：数组，风速观测数据
  - `wind_direction`：数组，风向观测数据

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "model_id": "xgboost_model_001",
    "corrected_data": {
      "temperature": [20.3, 20.8, 21.2, 21.7, 22.1],
      "humidity": [61, 59, 57, 55, 53],
      "wind_speed": [4.8, 5.0, 5.3, 5.6, 5.8],
      "wind_direction": [178, 183, 188, 193, 198]
    },
    "improvement": {
      "temperature": 0.95,
      "humidity": 0.92,
      "wind_speed": 0.90,
      "wind_direction": 0.88
    }
  }
}
```

### 3. 获取可用模型

**接口地址**：`GET /api/forecast/models`

**功能**：获取可用的预测和订正模型

**请求参数**：
- `type`：字符串，模型类型（"predict" 或 "correct"）

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "models": [
      {
        "id": "lstm_model_001",
        "name": "LSTM气象预测模型",
        "type": "predict",
        "accuracy": 0.92,
        "last_updated": "2026-04-01T00:00:00Z"
      },
      {
        "id": "xgboost_model_001",
        "name": "XGBoost气象订正模型",
        "type": "correct",
        "accuracy": 0.95,
        "last_updated": "2026-04-01T00:00:00Z"
      }
    ]
  }
}
```

### 4. 训练预测模型

**接口地址**：`POST /api/forecast/train`

**功能**：训练新的气象预测模型

**请求参数**：
- `model_name`：字符串，模型名称
- `model_type`：字符串，模型类型（"lstm" 或 "xgboost"）
- `training_data`：对象，训练数据
  - `variables`：对象，气象变量数据
    - `temperature`：数组，温度历史数据
    - `humidity`：数组，湿度历史数据
    - `wind_speed`：数组，风速历史数据
    - `wind_direction`：数组，风向历史数据
  - `labels`：对象，标签数据（仅用于监督学习）
    - `temperature`：数组，温度标签数据
    - `humidity`：数组，湿度标签数据
    - `wind_speed`：数组，风速标签数据
    - `wind_direction`：数组，风向标签数据
- `hyperparameters`：对象，超参数
  - `epochs`：数字，训练轮数
  - `batch_size`：数字，批次大小
  - `learning_rate`：数字，学习率

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "model_id": "new_lstm_model_001",
    "model_name": "新LSTM预测模型",
    "training_metrics": {
      "accuracy": 0.94,
      "loss": 0.02,
      "val_accuracy": 0.91,
      "val_loss": 0.03
    },
    "training_time": "00:15:30"
  }
}
```

### 5. 评估模型性能

**接口地址**：`POST /api/forecast/evaluate`

**功能**：评估模型的性能

**请求参数**：
- `model_id`：字符串，模型ID
- `test_data`：对象，测试数据
  - `variables`：对象，气象变量数据
    - `temperature`：数组，温度历史数据
    - `humidity`：数组，湿度历史数据
    - `wind_speed`：数组，风速历史数据
    - `wind_direction`：数组，风向历史数据
  - `labels`：对象，标签数据
    - `temperature`：数组，温度标签数据
    - `humidity`：数组，湿度标签数据
    - `wind_speed`：数组，风速标签数据
    - `wind_direction`：数组，风向标签数据

**响应**：
```json
{
  "code": "200",
  "message": "成功",
  "data": {
    "model_id": "lstm_model_001",
    "evaluation_metrics": {
      "temperature": {
        "rmse": 0.5,
        "mae": 0.3,
        "r2": 0.95
      },
      "humidity": {
        "rmse": 2.0,
        "mae": 1.5,
        "r2": 0.92
      },
      "wind_speed": {
        "rmse": 0.3,
        "mae": 0.2,
        "r2": 0.93
      },
      "wind_direction": {
        "rmse": 5.0,
        "mae": 3.0,
        "r2": 0.90
      }
    },
    "overall_accuracy": 0.93
  }
}
```

## 错误处理

| 错误代码 | 错误信息 | 描述 |
|---------|---------|------|
| 400 | 请求参数错误 | 缺少必要参数或参数格式不正确 |
| 401 | 未授权 | 缺少或无效的认证令牌 |
| 404 | 模型不存在 | 指定的model_id不存在 |
| 500 | 服务器内部错误 | 处理过程中发生错误 |

## 示例代码

### Python示例

```python
import requests
import json

# 执行气象预测
url = "http://localhost:8082/api/forecast/predict"
payload = {
    "model_id": "lstm_model_001",
    "input_data": {
        "variables": {
            "temperature": [20.0, 20.5, 21.0, 21.5, 22.0],
            "humidity": [60, 59, 58, 57, 56],
            "wind_speed": [5.0, 5.1, 5.2, 5.3, 5.4],
            "wind_direction": [180, 182, 184, 186, 188]
        },
        "time_steps": 5,
        "prediction_steps": 5
    }
}

response = requests.post(url, json=payload)
print(response.json())

# 执行气象数据订正
url = "http://localhost:8082/api/forecast/correct"
payload = {
    "model_id": "xgboost_model_001",
    "forecast_data": {
        "temperature": [20.5, 21.0, 21.5, 22.0, 22.5],
        "humidity": [60, 58, 56, 54, 52],
        "wind_speed": [5.0, 5.2, 5.5, 5.8, 6.0],
        "wind_direction": [180, 185, 190, 195, 200]
    },
    "observation_data": {
        "temperature": [20.3, 20.8, 21.2, 21.7, 22.1],
        "humidity": [61, 59, 57, 55, 53],
        "wind_speed": [4.8, 5.0, 5.3, 5.6, 5.8],
        "wind_direction": [178, 183, 188, 193, 198]
    }
}

response = requests.post(url, json=payload)
print(response.json())
```
