# grafana

Grafana 仪表板配置，为 UAV 平台提供 Prometheus / Elasticsearch / Jaeger 数据的可视化面板。

## 文件说明

```
grafana/
├── dashboards/
│   └── uav-platform-overview.json    # UAV 平台总览仪表板
└── provisioning/
    ├── dashboards/
    │   └── dashboard.yml             # 仪表板自动加载配置
    └── datasources/
        └── datasource.yml            # 数据源配置
```

## 预配置数据源

| 数据源 | 类型 | URL | 说明 |
|-------|------|-----|------|
| Prometheus | prometheus | `http://prometheus:9090` | 默认数据源，指标查询 |
| Elasticsearch | elasticsearch | `http://elasticsearch:9200` | 日志查询 (索引: `uav-logs-*`) |
| Jaeger | jaeger | `http://jaeger:16686` | 分布式链路追踪 |

## 预配置仪表板

### UAV Platform Overview

系统总览仪表板 (`uav-platform-overview.json`)，包含以下面板：

| 面板 ID | 标题 | 类型 | 说明 |
|:------:|------|------|------|
| 1 | Service Health | stat | 各服务在线状态 |
| 2 | HTTP Request Rate | graph | HTTP 请求速率趋势 |
| 3 | JVM Memory Usage | graph | JVM 堆内存使用 |
| 4 | Error Rate | graph | 5xx 错误率趋势 |
| 5 | Response Time P95 | graph | P95 响应延迟 |

## 快速开始

### 启动 Grafana

```bash
# 启动 Grafana (Docker Compose)
docker-compose -f deployments/monitoring/docker-compose.monitoring.yml up -d grafana

# 访问 Grafana
# http://localhost:3030
# 默认用户名: admin
# 默认密码: 从 GRAFANA_PASSWORD 环境变量读取
```

### Grafana API

```bash
# 健康检查
curl http://localhost:3000/api/health

# 列出所有仪表板
curl -u admin:$GRAFANA_PASSWORD http://localhost:3000/api/search?type=dash-db

# 导出仪表板
curl -u admin:$GRAFANA_PASSWORD http://localhost:3000/api/dashboards/uid/uav-platform-overview
```

## 安全配置

- `GF_SECURITY_ADMIN_USER=admin` — 管理员用户名
- `GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD}` — 密码从环境变量读取
- `GF_USERS_ALLOW_SIGN_UP=false` — 禁用用户注册
- `GF_SECURITY_ALLOW_EMBEDDING=false` — 禁止嵌入
- 运行用户: `472` (grafana 用户)，启用 `no-new-privileges`

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
