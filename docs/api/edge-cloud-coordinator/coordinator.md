# Edge-Cloud Coordinator API

边云协同服务 API - 任务调度、联邦学习、边云同步

## 基础信息

- **Base URL**: `http://localhost:8000`
- **WebSocket**: `ws://localhost:8765`
- **服务**: edge-cloud-coordinator
- **熔断器**: ✅ (HTTP/WebSocket/Federated Learning)

---

## 健康检查

### GET / - 根路径

**响应** (200):
```json
{
  "service": "Edge-Cloud Coordinator",
  "version": "1.0.0",
  "status": "running"
}
```

---

### GET /health - 健康检查

**响应** (200):
```json
{
  "status": "healthy"
}
```

---

## 任务管理 `/tasks`

### POST /tasks - 提交任务

提交边云协同计算任务

**请求体**:
```json
{
  "task_type": "global_path",
  "priority": 5,
  "data": {
    "drone_id": "UAV_001",
    "waypoints": [{"lat": 30.5, "lng": 114.3}]
  },
  "deadline": 60.0
}
```

**任务类型**:
- `global_path` - 全局路径规划
- `local_avoidance` - 局部避障
- `sensor_fusion` - 传感器融合
- `model_update` - 模型更新
- `batch_processing` - 批量处理

**响应** (200):
```json
{
  "task_id": "task_1",
  "status": "submitted",
  "message": "任务已提交到global_path队列"
}
```

---

### GET /tasks/{task_id} - 查询任务状态

**参数**: `task_id` - 任务ID

**响应** (200):
```json
{
  "task_id": "task_1",
  "task_type": "global_path",
  "priority": 5,
  "status": "completed",
  "result": {
    "optimized_path": [...],
    "distance": 85.5
  }
}
```

---

### DELETE /tasks/{task_id} - 取消任务

**响应** (200):
```json
{
  "message": "任务 task_1 已取消"
}
```

---

### GET /tasks - 获取任务列表

**查询参数**:
- `status` - 过滤状态 (pending/completed)
- `limit` - 返回数量 (默认10, 最大100)

**响应** (200):
```json
[
  {
    "task_id": "task_1",
    "task_type": "global_path",
    "priority": 5,
    "status": "completed"
  }
]
```

---

### POST /tasks/batch - 批量提交任务

**请求体**: 任务数组 (最多100个)

**响应** (200):
```json
{
  "results": [
    {"task_id": "task_1", "status": "submitted"},
    {"task_id": "task_2", "status": "submitted"}
  ]
}
```

---

## 边云协同 `/sync`

### GET /status - 获取系统状态

**响应** (200):
```json
{
  "node_id": "edge_node_001",
  "queue_size": 5,
  "completed_count": 100,
  "cloud_connected": true,
  "edge_connected": true,
  "buffer_size": 10
}
```

---

### POST /sync - 同步云端模型

同步云端模型到边缘节点

**响应** (200):
```json
{
  "message": "云端同步完成",
  "models": ["path_model_v1", "obstacle_detection_v2"]
}
```

---

### POST /upload - 上传边缘数据

后台上传边缘数据到云端

**响应** (200):
```json
{
  "message": "数据上传任务已提交"
}
```

---

### GET /models - 列出可用模型

**响应** (200):
```json
{
  "cloud_models": ["path_model_v1", "obstacle_detection_v2"],
  "local_models": ["local_path_v1"]
}
```

---

## 联邦学习 `/fl`

### POST /fl/update - 客户端更新

接收无人机客户端的联邦学习更新

**请求体**:
```json
{
  "drone_id": "UAV_001",
  "weights": {
    "w": [[1.0, 2.0], [3.0, 4.0]],
    "b": [0.5, 0.5]
  },
  "n_samples": 100,
  "metrics": {
    "accuracy": 0.95,
    "loss": 0.05
  }
}
```

**响应** (200):
```json
{
  "aggregated": true,
  "round_id": 5,
  "global_accuracy": 0.92
}
```

---

### GET /fl/status - 联邦学习状态

**响应** (200):
```json
{
  "strategy": "fedavg",
  "min_clients": 2,
  "round_id": 5,
  "clients_this_round": 3,
  "global_accuracy": 0.92,
  "total_rounds": 20
}
```

---

### GET /fl/history - 联邦学习历史

**响应** (200):
```json
{
  "rounds": [
    {"round_id": 1, "accuracy": 0.85, "clients": 3},
    {"round_id": 2, "accuracy": 0.88, "clients": 4}
  ]
}
```

---

### POST /fl/train - 本地训练

模拟无人机本地训练

**请求体**:
```json
{
  "drone_id": "UAV_001",
  "epochs": 5,
  "n_samples": 100
}
```

**响应** (200):
```json
{
  "drone_id": "UAV_001",
  "n_samples": 100,
  "metrics": {
    "accuracy": 0.95,
    "loss": 0.05
  },
  "aggregated": true
}
```

---

## 熔断器 API `/circuit-breaker`

### GET /api/circuit-breaker/status - 熔断器状态

**响应** (200):
```json
{
  "breakers": {
    "http": {
      "state": "CLOSED",
      "failures": 0,
      "successes": 100
    },
    "websocket": {
      "state": "CLOSED",
      "failures": 0,
      "successes": 50
    },
    "federated": {
      "state": "HALF_OPEN",
      "failures": 3,
      "successes": 10
    }
  }
}
```

---

### POST /api/circuit-breaker/trip/{name} - 手动触发熔断

**参数**: `name` - 熔断器名称 (http/websocket/federated)

**响应** (200):
```json
{
  "message": "熔断器 http 已触发"
}
```

---

### POST /api/circuit-breaker/reset/{name} - 重置熔断器

**参数**: `name` - 熔断器名称

**响应** (200):
```json
{
  "message": "熔断器 http 已重置"
}
```

---

## WebSocket `/ws/sync`

边云实时同步 WebSocket 端点

**连接地址**: `ws://localhost:8765/ws/sync`

**消息类型**:
- `path_update` - 路径更新
- `model_update` - 模型更新
- `task_status` - 任务状态
- `weather_alert` - 气象预警

---

## 错误响应

| 状态码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 404 | 任务/资源不存在 |
| 500 | 服务器内部错误 |

---

> **最后更新**: 2026-05-14
