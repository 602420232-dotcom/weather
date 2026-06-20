# ELK (Elasticsearch + Logstash + Kibana)

ELK 日志聚合栈配置，用于 UAV 平台所有微服务的日志集中收集、处理、存储与可视化分析。

## 文件说明

| 文件 | 说明 |
|------|------|
| `filebeat.yml` | Filebeat 日志采集配置 (Docker 容器日志) |
| `logstash.conf` | Logstash 日志处理管道配置 |

## 架构

```
Application Logs → Filebeat → Logstash → Elasticsearch → Kibana
```

### Filebeat (`filebeat.yml`)

- **输入**: 采集 Docker 容器的 JSON 格式日志 (`/var/lib/docker/containers/*/*.log`)
- **处理**: 解析 JSON 字段、添加 Docker 元数据、移除冗余字段 (`ecs`, `agent.hostname`)
- **输出**: 转发至 Logstash (`logstash:5044`)
- **日志级别**: `warning`

### Logstash (`logstash.conf`)

| 阶段 | 配置 | 说明 |
|------|------|------|
| **Input** | Beats (5044) | 接收 Filebeat 数据 |
| | TCP (5000) | 接收 JSON Lines 格式数据 |
| **Filter** | 条件标签 | Docker 来源添加 `docker-logs` 标签 |
| | JSON 解析 | 嵌套 JSON 字段展开 |
| | Mutate | 移除 `ecs`, `agent`, `host` 字段 |
| **Output** | Elasticsearch | 索引至 `uav-logs-YYYY.MM.dd` |
| | stdout | 调试输出 (rubydebug) |

## Elasticsearch 索引

日志自动按日期分片：

```
uav-logs-2026.05.14          # 普通日志
uav-logs-2026.05.14          # 错误日志 (按 level 过滤)
```

## 快速开始

### 启动 ELK 栈

```bash
# 启动基础设施 (包含 ELK + Nacos + SkyWalking)
docker-compose -f deployments/infrastructure.yml up -d

# 仅启动 ELK 组件
docker-compose -f deployments/infrastructure.yml up -d elasticsearch logstash kibana filebeat
```

### 访问界面

| 服务 | URL | 说明 |
|------|-----|------|
| Kibana | `http://localhost:5601` | 日志可视化与搜索 |
| Elasticsearch | `http://localhost:9200` | REST API |

### Kibana 使用

#### 创建索引模式

1. 进入 Kibana → Management → Stack Management → Index Patterns
2. 创建 `uav-logs-*` 索引模式
3. 设置 `@timestamp` 为时间字段

#### 常用查询

```
# 搜索错误日志
level:ERROR

# 搜索特定应用日志
logger_name:com.uav.platform

# 搜索用户操作
message:*login*

# 组合查询
level:ERROR AND logger_name:com.uav.auth

# 时间范围过滤
@timestamp:[now-1h TO now]
```

### 健康检查

```bash
# Elasticsearch 集群健康
curl http://localhost:9200/_cluster/health

# Elasticsearch 索引列表
curl http://localhost:9200/_cat/indices

# Kibana 状态
curl http://localhost:5601/api/status
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
