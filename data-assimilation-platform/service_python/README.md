# Data Assimilation Python Service

## 📋 服务概述

Python 微服务，提供数据同化算法的独立 API 接口。基于 FastAPI 构建，包含完整的中间件、路由、服务层架构。

**技术栈**:
- Python 3.8+
- FastAPI (Web 框架)
- Uvicorn (ASGI 服务器)
- NumPy, SciPy (数值计算)
- Pydantic (数据验证)

**服务端口**: 5000

---

## 📁 项目结构

```
service_python/
├── src/
│   └── api/
│       ├── core/                # 核心业务逻辑
│       │   └── assimilation_service.py  # 同化服务实现
│       ├── middleware/          # 中间件
│       │   ├── cors.py          # CORS 处理
│       │   ├── error_handler.py # 错误处理
│       │   └── logging.py       # 日志中间件
│       ├── models/              # 数据模型
│       │   ├── request.py       # 请求模型
│       │   └── response.py      # 响应模型
│       ├── parallel/            # 并行计算
│       │   └── dask.py          # Dask 分布式处理
│       ├── routes/              # API 路由
│       │   ├── assimilation.py  # 同化接口
│       │   ├── batch.py         # 批量处理
│       │   └── monitoring.py    # 监控接口
│       ├── services/            # 业务服务层
│       │   └── job_service.py   # 作业管理服务
│       ├── utils/               # 工具函数
│       │   ├── serializers.py   # 序列化工具
│       │   └── validators.py    # 数据验证
│       ├── main.py              # FastAPI 应用入口
│       └── multi_protocol.py    # 多协议支持
├── requirements/                # 依赖管理
│   ├── base.txt                 # 基础依赖
│   ├── dev.txt                  # 开发依赖
│   └── prod.txt                 # 生产依赖
├── tests/                       # 测试目录
│   └── test_variance_field.py
├── Dockerfile                   # Docker 构建
├── docker-compose.yml           # 本地开发编排
└── README.md                    # 本文档
```

---

## 🚀 快速开始

### 安装依赖

```bash
# 安装基础依赖
pip install -r requirements/base.txt

# 安装开发依赖
pip install -r requirements/dev.txt

# 安装生产依赖
pip install -r requirements/prod.txt
```

### 配置路径

该服务依赖 `algorithm_core` 模块，需要确保 Python 路径正确配置：
- 服务会自动在启动时配置路径指向 `../algorithm_core/src/`
- 或手动设置 PYTHONPATH 环境变量

### 运行服务

```bash
# 开发模式（带热重载）
cd src
uvicorn api.main:app --host 0.0.0.0 --port 5000 --reload

# 生产模式
uvicorn api.main:app --host 0.0.0.0 --port 5000 --workers 4
```

### API 文档

服务启动后，访问:
- Swagger UI: **http://localhost:5000/docs**
- ReDoc: **http://localhost:5000/redoc**

---

## 🔌 API 接口

### 同化接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/assimilation/execute` | POST | 执行贝叶斯同化 |
| `/api/v1/assimilation/batch` | POST | 批量执行同化 |
| `/api/v1/variance` | POST | 计算方差场 |
| `/health` | GET | 健康检查 |
| `/actuator/health` | GET | Actuator 健康检查 |

### 执行同化请求示例

```bash
curl -X POST http://localhost:5000/api/v1/assimilation/execute \
  -H "Content-Type: application/json" \
  -d '{
    "algorithm": "3dvar",
    "background": {
      "grid": {
        "lat": [],
        "lon": [],
        "lev": []
      },
      "data": []
    },
    "observations": [],
    "config": {
      "max_iterations": 10
    }
  }'
```

---

## 🧪 测试

```bash
# 运行所有测试
cd src
pytest -v

# 运行特定模块测试
pytest api/core/test_assimilation_service.py -v
```

---

## 🐳 Docker 部署

### 构建镜像

```bash
docker build -t data-assimilation-python:latest .
```

### 运行容器

```bash
docker run -d \
  --name data-assimilation-python \
  -p 5000:5000 \
  -v $(pwd)/../algorithm_core:/app/algorithm_core:ro \
  data-assimilation-python:latest
```

### 使用 Docker Compose

```bash
docker-compose up -d
```

---

## 🔧 配置说明

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `HOST` | 服务绑定地址 | 0.0.0.0 |
| `PORT` | 服务端口 | 5000 |
| `LOG_LEVEL` | 日志级别 | INFO |
| `ALGORITHM_CORE_PATH` | 算法库路径 | ../algorithm_core/src |

### CORS 配置

CORS 已配置为允许本地开发环境的跨域请求，生产环境建议限制来源。

---

## 📚 相关文档

| 文档 | 说明 |
|------|------|
| [Data Assimilation Platform README](../README.md) | 平台总览 |
| [Algorithm Core README](../algorithm_core/README.md) | 核心算法库文档 |
| [Core Module README](src/api/core/README.md) | 同化服务详细说明 |
| [Routes README](src/api/routes/README.md) | 路由接口文档 |

---

**最后更新**: 2026-06-02  
**版本**: 2.2  
**维护者**: DITHIOTHREITOL
