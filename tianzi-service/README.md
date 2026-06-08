# TianZi Weather Analysis Service

基于 TianZi 深度学习模型的高分辨率气象分析服务。

## 功能特性

- 高分辨率气象数据分析（最高 1km 分辨率）
- 支持多种分析模式：分析、预报、数据同化
- 轻量级风场查询接口（专为无人机路径规划优化）
- API Key + JWT 双重认证
- Spring Boot Actuator 兼容的健康检查

## 环境变量

| 变量名 | 必填 | 说明 | 默认值 |
|--------|------|------|--------|
| TIANZI_API_KEY | 生产环境必填 | API 密钥 | 空（开发环境禁用认证） |
| TIANZI_MODEL_PATH | 否 | 模型文件路径 | `/app/model/tianzi.onnx` |
| TIANZI_ENV | 否 | 运行环境 | `development` |
| CORS_ORIGINS | 生产环境必填 | 允许的 CORS 源 | `http://localhost:3000,http://localhost:8080` |
| JWT_SECRET | 否 | JWT 密钥（与 Java 后端共享） | 空 |

## API 端点

### 健康检查

```
GET /health
GET /actuator/health
GET /health/ready
```

### 气象分析

```
POST /api/v1/analysis
POST /api/v1/analysis/wind-field
GET /api/v1/model/info
```

## 请求示例

```bash
curl -X POST http://localhost:8090/api/v1/analysis \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "observation_data": [...],
    "analysis_type": "analysis",
    "resolution_km": 1.0
  }'
```

## 部署

### Docker

```bash
docker build -t tianzi-service .
docker run -p 8090:8090 \
  -e TIANZI_API_KEY=your-key \
  -v /path/to/model:/app/model \
  tianzi-service
```

### Docker Compose

```yaml
services:
  tianzi-service:
    build: ./tianzi-service
    ports:
      - "8090:8090"
    environment:
      - TIANZI_API_KEY=${TIANZI_API_KEY}
      - CORS_ORIGINS=${CORS_ORIGINS}
      - JWT_SECRET=${JWT_SECRET}
    volumes:
      - ./models/tianzi:/app/model
```

## 技术栈

- Python 3.11+
- FastAPI
- ONNX Runtime
- NumPy

## 开发

```bash
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8090 --reload
```

访问 Swagger UI: http://localhost:8090/docs