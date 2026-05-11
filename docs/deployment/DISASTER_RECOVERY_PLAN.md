# 灾备与灾难恢复方案 (Disaster Recovery Plan)
# UAV Path Planning System v1.0

## 1. 目标定义

| 指标 | 目标值 | 说明 |
|------|--------|------|
| **RTO** (恢复时间目标) | < 30 分钟 | 从故障发生到服务完全恢复 |
| **RPO** (恢复点目标) | < 1 小时 | 可容忍的最大数据丢失窗口 |
| **备份频率** | 每24小时全量 | MySQL + Nacos 配置 |
| **备份保留** | 7天本地 + 30天异地 | 多级保留策略 |

## 2. 备份策略

### 2.1 数据库备份 (MySQL)
- **方式**: mysqldump --all-databases + gzip
- **调度**: K8s CronJob 每日 02:00 UTC
- **存储**: PersistentVolumeClaim (50Gi)
- **异地**: rsync 至异地 S3/MinIO 存储（每周）

### 2.2 配置备份 (Nacos)
- **方式**: Nacos API 导出全量配置
- **调度**: K8s CronJob 每日 03:00 UTC
- **格式**: JSON

### 2.3 容器镜像
- **方式**: GitHub Container Registry (ghcr.io)
- **策略**: 保留最近10个版本标签

## 3. 恢复流程

### 3.1 数据库恢复
```bash
# 1. 从备份PVC获取备份文件
kubectl cp backup-pod:/backups/mysql/daily/ .

# 2. 解压并恢复
gunzip backup.sql.gz
mysql -h $MYSQL_HOST -u root -p < backup.sql
```

### 3.2 应用恢复
```bash
# K8s回滚到上一个版本
kubectl rollout undo deployment/uav-platform-service

# Docker Compose回滚
docker-compose down && docker-compose up -d
```

---

> **创建日期**: 2026-05-09  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL