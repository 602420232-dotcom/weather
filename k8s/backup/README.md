# backup

自动化备份脚本，支持 MySQL 数据库全量备份和 Nacos 配置备份，附带自动清理过期备份功能。

## 文件说明

| 文件 | 说明 |
|------|------|
| `backup.sh` | 自动化备份脚本 (bash) |

## 备份类型

| 类型 | 命令 | 说明 |
|------|------|------|
| MySQL 全量备份 | `backup_mysql()` | 全库备份 + 存储过程 + 触发器 |
| Nacos 配置备份 | `backup_nacos()` | Nacos 配置 API 导出为 JSON |
| 全备份 | `backup all` | MySQL + Nacos 同时备份 |

## MySQL 备份详情

| 参数 | 说明 |
|------|------|
| 工具 | `mysqldump` |
| 范围 | `--all-databases` |
| 事务 | `--single-transaction` (不锁表) |
| 内容 | `--routines` (存储过程) + `--triggers` (触发器) |
| 压缩 | gzip |
| 输出格式 | `mysql-YYYYMMDD-HHMMSS.sql.gz` |

## Nacos 备份详情

- API: `/nacos/v1/cs/configs`
- 输出格式: `nacos-YYYYMMDD-HHMMSS.json`
- 容错: Nacos 不可用时输出 WARN 而非失败

## 配置

脚本通过环境变量配置，无需修改代码：

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `BACKUP_DIR` | `<脚本目录>/data` | 备份文件存储目录 |
| `RETENTION_DAYS` | `7` | 备份保留天数 |
| `MYSQL_HOST` | `localhost` | MySQL 主机 |
| `MYSQL_PORT` | `3306` | MySQL 端口 |
| `MYSQL_USER` | `root` | MySQL 用户 |
| `MYSQL_PASSWORD` | (必需) | MySQL 密码 |
| `NACOS_URL` | `http://localhost:8848` | Nacos 服务地址 |

## 快速开始

### 使用

```bash
# 设置环境变量
export MYSQL_PASSWORD="your_secure_password"
export BACKUP_DIR="/data/backups"
export RETENTION_DAYS=30

# 全量备份 (MySQL + Nacos)
bash deployments/backup/backup.sh all

# 仅备份 MySQL
bash deployments/backup/backup.sh mysql

# 仅备份 Nacos
bash deployments/backup/backup.sh nacos
```

### 通过 Cron 定时执行

```bash
# 每天凌晨 2 点执行全量备份
0 2 * * * MYSQL_PASSWORD=xxx bash /path/to/backup.sh all >> /var/log/uav-backup.log 2>&1
```

### Kubernetes CronJob

参考 `deployments/kubernetes/backup-cronjob.yml` 使用 Kubernetes CronJob 在集群内运行备份任务。

## 自动化清理

脚本执行完成后自动清理超过 `RETENTION_DAYS` 天的备份文件：

```bash
# 默认清理 7 天前的备份
find /data/backups -name "mysql-*.sql.gz" -mtime +7 -delete
find /data/backups -name "nacos-*.json" -mtime +7 -delete
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
