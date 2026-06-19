# streaming

流处理与消息队列基础设施配置，基于 Kafka + Flink 实现 UAV 平台实时数据流处理。

## 文件说明

| 文件 | 说明 |
|------|------|
| `docker-compose.stream.yml` | Kafka + Flink 流处理 Docker Compose |

## 组件

| 组件 | 镜像 | 端口 | 说明 |
|------|------|:----:|------|
| Zookeeper | `confluentinc/cp-zookeeper:7.4.0` | 2181 | Kafka 集群协调 |
| Kafka Broker | `confluentinc/cp-kafka:7.4.0` | 9092 / 29092 | 消息队列核心 |
| Kafka UI | `provectuslabs/kafka-ui:latest` | 8087 | Kafka Web 管理界面 |
| Flink JobManager | `flink:1.18.0` | 8085 | 流作业调度管理 |
| Flink TaskManager | `flink:1.18.0` | — | 流作业执行节点 |

## 资源限制

| 组件 | Memory Limit | Memory Reservation |
|------|:-----------:|:-----------------:|
| Zookeeper | — | — |
| Kafka Broker | 1G | — |
| Flink JobManager | 1G | — |
| Flink TaskManager | 1G | — |

## Kafka 配置

| 参数 | 值 | 说明 |
|------|-----|------|
| `KAFKA_ADVERTISED_LISTENERS` | `PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:29092` | 内外网监听 |
| `KAFKA_INTER_BROKER_LISTENER_NAME` | `PLAINTEXT_INTERNAL` | Broker 间通信协议 |
| `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` | 1 | Offset Topic 副本数 |

## 快速开始

### 启动流处理栈

```bash
docker-compose -f deployments/streaming/docker-compose.stream.yml up -d
```

### 验证服务

```bash
# Kafka UI
open http://localhost:8087

# Flink Dashboard
open http://localhost:8085

# 检查 Kafka Broker
docker exec uav-kafka-stream kafka-broker-api-versions --bootstrap-server localhost:9092

# 查看 Kafka Topics
docker exec uav-kafka-stream kafka-topics --bootstrap-server localhost:9092 --list
```

### 创建 Topic

```bash
# 创建气象数据 Topic
docker exec uav-kafka-stream kafka-topics --bootstrap-server localhost:9092 \
  --create --topic uav-weather-data --partitions 3 --replication-factor 1

# 创建路径规划 Topic
docker exec uav-kafka-stream kafka-topics --bootstrap-server localhost:9092 \
  --create --topic uav-path-planning --partitions 3 --replication-factor 1

# 创建边缘数据 Topic
docker exec uav-kafka-stream kafka-topics --bootstrap-server localhost:9092 \
  --create --topic uav-edge-data --partitions 3 --replication-factor 1
```

## 典型流处理场景

- **实时气象数据处理**: 气象采集 → Kafka → Flink → 数据同化
- **边缘-云端同步**: 边缘设备 → Kafka → 云端聚合
- **路径规划事件流**: 规划请求 → Kafka → Flink → 结果分发
- **实时告警**: 监控指标 → Kafka → Flink CEP → Alertmanager

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
