# FengWu Weather Forecast Service

## 概述

风乌（FengWu）气象预测服务，基于 ONNX 推理引擎运行深度学习全球天气预报模型。该服务提供 REST API 接口，接收 ERA5 大气数据并输出 0~14 天的全球气象预报。

## 技术栈

| 技术 | 版本 | 用途 |
|------|:----:|------|
| Python | 3.11 | 运行环境 |
| FastAPI | >=0.110.0 | Web 框架 |
| Uvicorn | >=0.29.0 | ASGI 服务器 |
| ONNX Runtime | >=1.17.0 | 深度学习模型推理 |
| NumPy | >=1.26.0 | 数值计算 |

## 项目结构

```
fengwu-service/
├── app.py                 # FastAPI 应用主入口
├── inference_engine.py    # ONNX 推理引擎封装
├── requirements.txt       # Python 依赖
├── Dockerfile             # Docker 镜像构建
└── README.md              # 本文件
```

## 服务端口

| 端口 | 协议 | 说明 |
|:----:|------|------|
| 8085 | HTTP | REST API 服务端口 |

## API 端点

### 健康检查

```
GET /health
GET /health/ready
```

### 全球天气预报

```
POST /api/v1/forecast
```

**请求体:**
```json
{
  "input_0h": [[[...]]],
  "input_6h": [[[...]]],
  "steps": 56,
  "surface_only": true
}
```

**参数说明:**
- `input_0h`: T+0h 时刻的 ERA5 大气数据，形状 (69, 721, 1440)
- `input_6h`: T+6h 时刻的 ERA5 大气数据，形状 (69, 721, 1440)
- `steps`: 预测步数，1~56，每步 6 小时
- `surface_only`: 仅返回地表变量（u10、v10、t2m、msl）

### 风场快速查询

```
POST /api/v1/forecast/wind
```
轻量级端点，仅返回网格级风速/风向摘要，适用于无人机路径规划快速查询。

### 模型信息

```
GET /api/v1/model/info
```

## 69 个变量说明

变量按以下顺序排列（13 个气压层 × 5 个变量 + 4 个地表变量）:

| 索引范围 | 变量 |
|---------|------|
| 0-3 | 地表: u10, v10, t2m, msl |
| 4-8 | 50hPa: z, q, u, v, t |
| 9-13 | 100hPa: z, q, u, v, t |
| ... | 500hPa, 850hPa, 1000hPa 等共13层 |

## Docker 部署

### 构建镜像

```bash
docker build -t uav/fengwu-service:latest -f fengwu-service/Dockerfile .
```

### 运行容器

```bash
docker run -d \
  --name fengwu-service \
  -p 8085:8085 \
  -v /path/to/model:/app/model \
  uav/fengwu-service:latest
```

### 环境要求

- 模型文件需挂载到容器的 `/app/model/` 目录
- 推荐使用 GPU 加速（通过 NVIDIA Container Toolkit）

## 开发指南

### 安装依赖

```bash
pip install -r fengwu-service/requirements.txt
```

### 启动开发服务器

```bash
cd fengwu-service
uvicorn app:app --host 0.0.0.0 --port 8085 --reload
```

### API 文档

启动后访问: http://localhost:8085/docs (FastAPI 自动生成的 Swagger UI)

## 相关文档

- [根项目 README](../README.md)
- [端口配置总表](../docs/PORTS_CONFIGURATION.md)
- [项目架构文档](../docs/architecture.md)

---

> **最后更新**: 2026-05-30
> **版本**: 1.0
> **维护者**: UAV Platform Team
