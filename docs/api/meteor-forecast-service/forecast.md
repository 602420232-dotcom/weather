# 气象预测服务API

气象预测服务提供气象数据的预测和订正功能，使用LSTM和XGBoost模型进行气象预测。

## 接口列表

### 1. 执行气象预测

**接口地址**：`POST /api/forecast/predict`

**功能**：执行气象预测

**请求参数**：JSON
```json
{
  "data": [...],
  "method": "lstm"
}
```

**响应**：
```json
{
  "success": true,
  "data": { "predictions": [...] }
}
```

### 2. 气象数据订正

**接口地址**：`POST /api/forecast/correct`

**功能**：执行气象数据订正

**请求参数**：JSON
```json
{
  "forecast_data": [...],
  "observed_data": [...]
}
```

**响应**：
```json
{
  "success": true,
  "data": { "corrected": [...] }
}
```

### 3. 获取可用模型列表

**接口地址**：`GET /api/forecast/models`

**功能**：获取可用模型列表

**响应**：
```json
{
  "success": true,
  "data": { "models": ["lstm", "xgboost", "convlstm", "gpr"] }
}
```

### 4. 获取气象预测

**接口地址**：`GET /api/forecast/get`

**功能**：根据经纬度获取气象预测

**请求参数**：Query
- `lat`：纬度
- `lng`：经度
- `hours`：预测时长（小时）

**响应**：
```json
{
  "success": true,
  "data": { "temperature": 25.5, "wind_speed": 10.2, "humidity": 65 }
}
```

### 5. 获取详细气象预测

**接口地址**：`POST /api/forecast/detail`

**功能**：获取详细的气象预测数据

**请求参数**：JSON
```json
{
  "lat": 39.9042,
  "lng": 116.4074,
  "hours": 24,
  "parameters": ["temperature", "wind_speed", "humidity", "pressure"]
}
```

**响应**：
```json
{
  "success": true,
  "data": {
    "forecast": [...],
    "metadata": { "model": "lstm", "confidence": 0.95 }
  }
}
```

### 6. 获取实时天气

**接口地址**：`GET /api/forecast/realtime`

**功能**：获取实时天气数据

**请求参数**：Query
- `lat`：纬度
- `lng`：经度

**响应**：
```json
{
  "success": true,
  "data": {
    "temperature": 26.0,
    "wind_speed": 8.5,
    "wind_direction": 180,
    "humidity": 70,
    "pressure": 1013.25,
    "visibility": 10,
    "timestamp": "2024-01-01T12:00:00Z"
  }
}
```

### 7. 健康检查

**接口地址**：`GET /actuator/health`

**功能**：服务健康检查

**响应**：
```json
{
  "status": "UP"
}
```

---

> **最后更新**: 2026-06-05  
> **版本**: 2.2  
> **维护者**: DITHIOTHREITOL
