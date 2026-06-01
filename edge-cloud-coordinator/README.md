# 边云协同计算框架

## 概述

边云协同计算框架（edge-cloud-coordinator）实现无人机与云端之间的分布式智能计算，支持任务编排、实时流处理、联邦学习、WebSocket同步和AI推理。

## 模块说明

| 模块 | 文件 | 说明 |
|------|------|------|
| 边云协调器 | [coordinator.py](coordinator.py) | 任务编排、云端/边缘分流、增量学习 |
| REST API 服务 | [api.py](api.py) | FastAPI 接口，提供边云协调 API |
| 熔断器 | [circuit_breaker.py](circuit_breaker.py) | 服务熔断机制，防止级联故障 |
| 熔断器 API | [circuit_breaker_api.py](circuit_breaker_api.py) | 熔断器状态管理接口 |
| 实时流处理 | [realtime_stream.py](realtime_stream.py) | Kafka/RabbitMQ流处理、秒级风险评估 |
| 边缘AI推理 | [edge_ai_inference.py](edge_ai_inference.py) | TensorRT/ONNX INT8量化、推理基准 |
| 联邦学习 | [federated_learning.py](federated_learning.py) | FedAvg/FedProx多无人机协同学习 |
| WebSocket同步 | [websocket_sync.py](websocket_sync.py) | 实时双向状态同步 |
| 安全增强 | [security.py](security.py) | mTLS服务间通信、JWT认证、数据加密 |
| 安全验证 | [security_validation.py](security_validation.py) | 安全策略验证与审计 |
| 知识图谱 | [knowledge_graph.py](knowledge_graph.py) | 气象+路径知识图谱推理 |
| AI决策 | [ai_decision.py](ai_decision.py) | LLM辅助决策、智能问答 |
| V2X协同 | [v2x_cooperative.py](v2x_cooperative.py) | 协同感知、蜂群共识 |
| 气象采集 | [uav_weather_collector.py](uav_weather_collector.py) | 多源气象数据采集 |
| 流处理器 | [stream_processor.py](stream_processor.py) | Flink SQL作业管理 |
| 网络推理 | [network_inference.py](network_inference.py) | 网络拓扑与通信优化 |

## 架构

```
云端 ──────────────┬──────────────┐
                  │              │
          全局路径规划        批量计算
                  │              │
    ┌─────────────┴──────────────┘
    │  WebSocket 实时同步
    │  Kafka/RabbitMQ 流处理
    │  mTLS 安全通道
    ▼
边缘 ──────────────┬──────────────┐
                  │              │
          实时避障        传感器融合
                  │              │
          联邦学习         INT8推理
```

## 数据流

```
气象传感器 → Kafka(weather-raw) → Flink流处理 → 风险评估 → 动态路径调整
                                              ↓
                                         告警通知
```

## 快速开始

### 环境要求

- Python 3.8+
- FastAPI & Uvicorn (API 服务)
- kafka-python, websockets, pika (可选中间件)

### 安装依赖

```bash
pip install -r requirements.txt
```

### 运行服务

#### 运行边云协调器

```bash
# 直接运行协调器
python coordinator.py

# 或运行 REST API 服务
uvicorn api:app --host 0.0.0.0 --port 8000 --reload
```

#### 服务端口

| 端口 | 协议 | 说明 |
|:----:|------|------|
| 8000 | HTTP | REST API 服务端口 |
| 8765 | WebSocket | 实时同步端口 |

### API 文档

启动后访问:
- Swagger UI: **http://localhost:8000/docs**

## API 接口

### 主要端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/health` | GET | 健康检查 |
| `/api/v1/task/submit` | POST | 提交任务 |
| `/api/v1/task/status/{task_id}` | GET | 查询任务状态 |
| `/api/v1/federated/init` | POST | 初始化联邦学习 |
| `/api/v1/federated/update` | POST | 更新联邦学习模型 |
| `/api/v1/circuit-breaker/status` | GET | 熔断器状态 |

## 测试

```bash
# 运行协调器测试
python test_coordinator.py

# 运行边缘协调器测试
python test_edge_coordinator.py
```

## Docker 部署

### 构建镜像

```bash
docker build -t uav/edge-cloud-coordinator:latest .
```

### 运行容器

```bash
docker run -d \
  --name edge-cloud-coordinator \
  -p 8000:8000 \
  -p 8765:8765 \
  uav/edge-cloud-coordinator:latest
```

## 相关文档

- [根项目 README](../README.md)
- [API 规范](../docs/api/edge-cloud-coordinator/coordinator.md)
- [端口配置总表](../docs/PORTS_CONFIGURATION.md)

---

> **最后更新**: 2026-06-02  
> **版本**: 2.2  
> **维护者**: DITHIOTHREITOL
