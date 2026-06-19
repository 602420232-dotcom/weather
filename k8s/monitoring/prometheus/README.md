# prometheus

Prometheus 监控与告警配置，负责 UAV 平台所有微服务的指标采集、存储与告警规则评估。

## 文件说明

| 文件 | 说明 |
|------|------|
| `prometheus.yml` | Prometheus 主配置 (抓取目标/规则文件) |
| `alerts.yml` | Prometheus 告警规则定义 |

## 采集目标

### Spring Boot 应用 (Java)

| Job 名称 | 目标服务 | 端口 | Metrics 路径 |
|---------|---------|:---:|------------|
| `uav-platform` | 无人机平台服务 | 8080 | `/actuator/prometheus` |
| `data-assimilation` | 数据同化服务 | 8084 | `/actuator/prometheus` |
| `meteor-forecast` | 气象预测服务 | 8082 | `/actuator/prometheus` |
| `path-planning` | 路径规划服务 | 8083 | `/actuator/prometheus` |
| `wrf-processor` | WRF 处理服务 | 8081 | `/actuator/prometheus` |
| `uav-weather` | 气象采集服务 | 8086 | `/actuator/prometheus` |

### 基础设施

| Job 名称 | 目标服务 | 端口 |
|---------|---------|:---:|
| `mysql` | MySQL Exporter | 9104 |
| `redis` | Redis Exporter | 9121 |
| `nginx` | Nginx Exporter | 9113 |
| `cadvisor` | 容器监控 (cAdvisor) | 8080 |

### Kubernetes Pod

自动发现带有 `prometheus.io/scrape: true` 注解的 Pod。

## 告警规则

告警分为六个分组，覆盖基础设施到业务层：

### 基础设施告警 (`uav_platform_alerts`)

| 告警 | 级别 | 条件 |
|------|:----:|------|
| `HighCPUUsage` | warning | CPU > 80% 持续 5 分钟 |
| `CriticalCPUUsage` | critical | CPU > 95% 持续 2 分钟 |
| `HighMemoryUsage` | warning | 内存 > 85% 持续 5 分钟 |
| `CriticalMemoryUsage` | critical | 内存 > 95% 持续 1 分钟 |
| `HighDiskUsage` | warning | 磁盘 > 85% |
| `DiskSpaceCritical` | critical | 磁盘 > 95% |

### 服务告警 (`uav_service_alerts`)

| 告警 | 级别 | 条件 |
|------|:----:|------|
| `ServiceDown` | critical | 服务离线 > 1 分钟 |
| `HighErrorRate` | warning | 5xx 错误率 > 5% |
| `CriticalErrorRate` | critical | 5xx 错误率 > 20% |
| `HighResponseTime` | warning | P95 延迟 > 2s |
| `CriticalResponseTime` | critical | P99 延迟 > 5s |
| `HighRequestRate` | info | 请求率 > 1000 req/s |

### 数据库告警 (`database_alerts`)

| 告警 | 级别 | 条件 |
|------|:----:|------|
| `MySQLDown` | critical | MySQL 不可用 |
| `MySQLHighConnectionUsage` | warning | 连接使用率 > 80% |
| `MySQLSlowQueries` | warning | 慢查询 > 10/s |

### 缓存告警 (`cache_alerts`)

| 告警 | 级别 | 条件 |
|------|:----:|------|
| `RedisDown` | critical | Redis 不可用 |
| `RedisHighMemoryUsage` | warning | 内存使用 > 80% |
| `RedisHighConnectionUsage` | warning | 连接数 > 1000 |

### 业务告警 (`business_alerts`)

| 告警 | 级别 | 条件 |
|------|:----:|------|
| `HighAuthFailureRate` | warning | 认证失败 > 10/分钟 |
| `PossibleBruteForceAttack` | critical | 5 分钟内 > 50 次认证失败 |
| `WeatherDataCollectionFailure` | warning | 气象采集失败率 > 10% |
| `PathPlanningFailure` | warning | 路径规划失败率 > 5% |

### 扩缩容告警 (`scaling_alerts`)

| 告警 | 级别 | 条件 |
|------|:----:|------|
| `ConsiderScalingUp` | info | CPU > 70% 持续 10 分钟 |
| `ConsiderScalingDown` | info | CPU < 20% 持续 30 分钟 |

## 快速开始

```bash
# 启动 Prometheus (Docker Compose)
docker-compose -f deployments/monitoring/docker-compose.monitoring.yml up -d prometheus

# 查看 Prometheus 界面
open http://localhost:9090

# 查看告警状态
curl http://localhost:9090/api/v1/alerts

# 验证采集目标
curl http://localhost:9090/api/v1/targets
```

## 常用 PromQL 查询

```promql
# 服务健康状态
up{job=~"uav-platform|path-planning|wrf-processor"}

# HTTP 请求率 (5分钟)
rate(http_server_requests_seconds_count[5m])

# 5xx 错误率
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# P95 响应时间
http_server_requests_seconds{quantile="0.95"}

# JVM 堆内存使用
jvm_memory_used_bytes{area="heap"}

# CPU 使用率
cpu_usage_percent
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
