# observability

可观测性统一部署配置，整合 Prometheus + Grafana + Jaeger 三大组件，为 UAV 平台提供指标采集、可视化仪表板和分布式链路追踪能力。

## 文件说明

| 文件 | 说明 |
|------|------|
| `observability.yml` | Kubernetes 可观测性部署 (Prometheus + Grafana + Jaeger) |

## 组件

| 组件 | 镜像 | 端口 | 命名空间 |
|------|------|:---:|---------|
| Jaeger | `jaegertracing/all-in-one:1.54` | 16686 / 4317 / 4318 / 9411 | `uav-observability` |
| Prometheus | ConfigMap + Deployment | 9090 | `uav-observability` |
| Grafana | `grafana/grafana:10.2.0` | 3000 | `uav-observability` |

### Jaeger

| 端口 | 用途 |
|:---:|------|
| 16686 | Jaeger UI 界面 |
| 4317 | OTLP gRPC |
| 4318 | OTLP HTTP |
| 9411 | Zipkin 兼容 |

### Prometheus

**采集目标** (通过 ConfigMap 配置 `prometheus.yml`):

| Job | 目标服务 | 路径 |
|-----|---------|------|
| `uav-microservices` | wrf-processor(8081), meteor-forecast(8082), path-planning(8083), data-assimilation(8084), uav-platform(8080), api-gateway(8088), uav-weather-collector(8086) | `/actuator/prometheus` |
| `edge-cloud-coordinator` | edge-cloud-coordinator:8000 | `/metrics` |
| `jaeger` | jaeger:16686 | — |
| `node-exporter` | node-exporter:9100 | — |

**内置告警规则**:
- `ServiceDown`: 服务离线 > 1 分钟 (critical)
- `HighMemoryUsage`: JVM 内存 > 85% (warning)

### Grafana

**数据源** (通过 ConfigMap 自动配置):

| 数据源 | 类型 | URL |
|-------|------|-----|
| Prometheus | prometheus | `http://prometheus:9090` |
| Jaeger | jaeger | `http://jaeger:16686` |
| Elasticsearch | elasticsearch | `http://elasticsearch:9200` (索引: `[uav-logs-]YYYY.MM.DD`) |

**安全**:
- 匿名访问已启用
- Admin 密码通过 Secret `grafana-secret` 管理

## 快速开始

### 部署

```bash
# Kubernetes 部署
kubectl apply -f deployments/observability/observability.yml

# Docker 部署
docker-compose -f deployments/monitoring/docker-compose.monitoring.yml up -d
```

### 访问

| 服务 | URL | 说明 |
|------|-----|------|
| Grafana | `http://localhost:3000` | 指标仪表板 |
| Prometheus | `http://localhost:9090` | 指标查询 |
| Jaeger | `http://localhost:16686` | 链路追踪 |

### 验证

```bash
# Prometheus targets
curl http://localhost:9090/api/v1/targets

# Grafana health
curl http://localhost:3000/api/health

# Jaeger services
curl http://localhost:16686/api/services
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
