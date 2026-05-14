# multi-region

多区域容灾部署管理器，实现跨区域服务注册、健康检查、故障切换与数据同步。

## 文件说明

| 文件 | 说明 |
|------|------|
| `deployer.py` | 多区域部署管理器 (Python) |

## 核心功能

### MultiRegionDeployer 类

| 方法 | 说明 |
|------|------|
| `add_region(name, endpoint, priority)` | 注册新区域节点 |
| `start_health_check()` | 启动周期性健康检查 |
| `failover(region_name)` | 手动/自动故障切换 |
| `sync_data(source, target)` | 区域间数据同步 |

### 区域状态 (`RegionStatus`)

| 状态 | 说明 |
|------|------|
| `ACTIVE` | 当前活跃区域，接收所有流量 |
| `STANDBY` | 备用区域，待命就绪 |
| `FAILED` | 故障区域，已触发切换 |
| `SYNCING` | 数据同步中 |

### Region 数据模型

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | `str` | 区域名称 (如 `cn-east-1`) |
| `endpoint` | `str` | 区域服务端点 |
| `status` | `RegionStatus` | 当前状态 |
| `priority` | `int` | 优先级 (数值越大越高) |
| `last_heartbeat` | `float` | 最后心跳时间戳 |
| `load` | `float` | 当前负载 |

## 快速开始

```python
from multi_region import MultiRegionDeployer

deployer = MultiRegionDeployer()

# 注册多区域
deployer.add_region("cn-east-1", "https://api-east.uav-platform.com", priority=1)
deployer.add_region("cn-west-1", "https://api-west.uav-platform.com", priority=2)
deployer.add_region("ap-southeast-1", "https://api-sea.uav-platform.com", priority=1)

# 启动健康检查
deployer.start_health_check()

# 手动故障切换
deployer.failover("cn-west-1")
```

## 容灾架构

```
           DNS / GSLB
              │
      ┌───────┴───────┐
      │               │
  Region A (ACTIVE)  Region B (STANDBY)
      │               │
      ├── MySQL       ├── MySQL (replica)
      ├── Redis       ├── Redis (standby)
      └── Services    └── Services (warm)
```

## 同步策略

- **同步间隔**: 默认 5 秒
- **健康检查**: 周期性心跳检测
- **优先故障切换**: 自动选择优先级最高的健康区域

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
