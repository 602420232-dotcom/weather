# edge-device

边缘设备部署配置，用于在资源受限的边缘节点 (如无人机机载计算机) 上运行 AI 推理与实时数据采集服务。

## 文件说明

| 文件 | 说明 |
|------|------|
| `Dockerfile` | 边缘设备 Python 服务镜像构建 |
| `docker-compose.edge.yml` | 边缘设备 Docker Compose |
| `requirements-edge.txt` | 边缘设备 Python 依赖 |

## Docker 镜像

### 基础信息

| 属性 | 值 |
|------|-----|
| 基础镜像 | `python:3.11-slim` |
| 运行时用户 | `appuser` (非 root) |
| 工作目录 | `/app` |
| 暴露端口 | 8080 (HTTP API), 8765 (WebSocket) |
| 健康检查 | `http://localhost:8080/health` (30s 间隔) |

### 构建依赖

- `gcc`, `g++`, `cmake` — ONNX runtime 编译依赖
- `libgl1-mesa-glx`, `libglib2.0-0` — 图像处理库

## Python 依赖

| 包 | 版本 | 用途 |
|----|------|------|
| `numpy` | ≥1.21.0 | 数值计算 |
| `scipy` | ≥1.7.0 | 科学计算 |
| `pandas` | ≥1.3.0 | 数据处理 |
| `fastapi` | ≥0.85.0 | HTTP API 服务 |
| `uvicorn` | ≥0.18.0 | ASGI 服务器 |
| `pydantic` | ≥1.9.0 | 数据验证 |
| `websockets` | ≥10.0 | WebSocket 通信 |
| `requests` | ≥2.27.0 | HTTP 客户端 |
| `kafka-python` | ≥2.0.2 | Kafka 消息队列 |
| `onnxruntime` | ≥1.12.0 | ONNX 模型推理 |
| `psutil` | ≥5.9.0 | 系统监控 |

## 资源限制

| 资源 | Limit | Reservation |
|------|:-----:|:----------:|
| Memory | 256M | 128M |
| CPU | 1.0 | 0.5 |

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka 集群地址 |
| `MODEL_PRECISION` | `int8` | AI 模型推理精度 |
| `INFERENCE_BACKEND` | `onnx` | 推理后端引擎 |
| `EDGE_NODE_ID` | `edge-001` | 边缘节点标识 |

## 快速开始

### 构建镜像

```bash
docker build -f deployments/edge-device/Dockerfile -t uav-edge-device .
```

### 启动边缘设备

```bash
# 启动边缘服务
docker-compose -f deployments/edge-device/docker-compose.edge.yml up -d

# 查看日志
docker-compose -f deployments/edge-device/docker-compose.edge.yml logs -f

# 验证健康
curl http://localhost:8080/health
```

### 安装依赖 (本地开发)

```bash
pip install -r deployments/edge-device/requirements-edge.txt
```

## 安全特性

- 非 root 用户运行 (`appuser`)
- 内存限制 256M 防止 OOM
- 镜像已最小化 (slim 基础镜像)
- 构建依赖在安装后清理 (`rm -rf /var/lib/apt/lists/*`)

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
