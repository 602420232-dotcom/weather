# database

数据库优化与运维配置，包含 MySQL 索引创建和数据库参数调优脚本。

## 文件说明

| 文件 | 说明 |
|------|------|
| `optimize.sql` | 数据库性能优化脚本 (索引 + 系统参数) |

## 索引优化

`optimize.sql` 为以下表创建索引以加速查询：

### 用户与角色

| 表 | 索引 | 字段 | 用途 |
|----|------|------|------|
| `user` | `idx_user_username` | `username` | 用户名登录查询 |
| `user` | `idx_user_email` | `email` | 邮箱查询 |
| `user` | `idx_user_status` | `enabled` | 用户状态筛选 |
| `role` | `idx_role_name` | `name` | 角色名查询 |

### 任务与无人机

| 表 | 索引 | 字段 | 用途 |
|----|------|------|------|
| `task` | `idx_task_status` | `status` | 任务状态筛选 |
| `task` | `idx_task_start_time` | `start_time` | 按开始时间排序 |
| `task` | `idx_task_end_time` | `end_time` | 按结束时间排序 |
| `task` | `idx_task_created_at` | `created_at` | 按创建时间排序 |
| `drone` | `idx_drone_status` | `status` | 无人机状态筛选 |
| `drone` | `idx_drone_battery` | `battery_level` | 电池电量筛选 |

### 路径规划

| 表 | 索引 | 字段 | 用途 |
|----|------|------|------|
| `path_planning` | `idx_path_task_id` | `task_id` | 按任务关联查询 |
| `path_planning` | `idx_path_drone_id` | `drone_id` | 按无人机关联查询 |
| `path_planning` | `idx_path_status` | `status` | 规划状态筛选 |
| `path_planning` | `idx_path_created_at` | `created_at` | 按创建时间排序 |

### 历史记录与气象

| 表 | 索引 | 字段 | 用途 |
|----|------|------|------|
| `task_history` | `idx_history_task_id` | `task_id` | 任务历史关联 |
| `task_history` | `idx_history_drone_id` | `drone_id` | 无人机历史关联 |
| `task_history` | `idx_history_status` | `status` | 历史状态筛选 |
| `task_history` | `idx_history_start_time` | `start_time` | 按开始时间查询 |
| `task_history` | `idx_history_end_time` | `end_time` | 按结束时间查询 |
| `weather_data` | `idx_weather_location` | `latitude, longitude` | 地理坐标查询 |
| `weather_data` | `idx_weather_timestamp` | `timestamp` | 按时间查询 |

## 数据库参数调优

| 参数 | 值 | 说明 |
|------|:--:|------|
| `innodb_buffer_pool_size` | 1G | InnoDB 缓冲池大小 |
| `innodb_flush_log_at_trx_commit` | 2 | 日志刷新策略 (平衡性能与安全) |
| `innodb_log_buffer_size` | 16M | 日志缓冲区 |
| `innodb_log_file_size` | 256M | 日志文件大小 |
| `innodb_stats_on_metadata` | OFF | 禁用元数据统计 |
| `query_cache_size` | 64M | 查询缓存大小 |
| `query_cache_type` | 1 | 启用查询缓存 |
| `query_cache_limit` | 2M | 单查询缓存上限 |
| `max_connections` | 200 | 最大连接数 |
| `wait_timeout` | 300 | 等待超时 (秒) |
| `interactive_timeout` | 300 | 交互超时 (秒) |

## 使用方法

```bash
# 执行优化脚本
mysql -u root -p < deployments/database/optimize.sql

# 或通过 Docker
docker exec -i uav-mysql mysql -u root -p < deployments/database/optimize.sql

# 验证索引
mysql -u root -p -e "SHOW INDEX FROM path_planning;"
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
