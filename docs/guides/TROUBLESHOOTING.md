# 故障排除指南

本文档提供常见问题的排查和解决方案。

---

## 目录

- [服务启动问题](#服务启动问题)
- [数据库问题](#数据库问题)
- [网络连接问题](#网络连接问题)
- [性能问题](#性能问题)
- [算法问题](#算法问题)

---

## 服务启动问题

### 服务一直处于 restarting 状态

**排查步骤：**
```bash
# 查看服务日志
docker-compose logs [service-name]

# 检查依赖服务是否正常
docker-compose ps

# 检查端口是否被占用
netstat -tulpn | grep [port]
```

**常见原因：**
| 原因 | 解决方案 |
|------|----------|
| 数据库连接失败 | 检查 `DB_PASSWORD` 配置是否正确 |
| Nacos未启动 | 等待Nacos完全启动后再启动其他服务 |
| 端口被占用 | 修改端口或停止占用程序 |
| 配置错误 | 检查 `.env` 文件配置 |

### Java服务启动失败

**排查步骤：**
```bash
# 查看详细日志
docker-compose logs [service-name] --tail=100

# 检查健康状态
curl http://localhost:[port]/actuator/health
```

**常见原因：**
- Maven依赖下载失败 → 检查网络连接
- 端口冲突 → 修改端口映射
- 内存不足 → 增加Docker资源限制

### Python算法服务启动失败

**排查步骤：**
```bash
# 检查Python环境
docker-compose exec [service] python --version

# 检查依赖安装
docker-compose exec [service] pip list
```

---

## 数据库问题

### MySQL连接失败

**排查步骤：**
```bash
# 检查MySQL服务状态
docker-compose ps mysql

# 测试MySQL连接
docker-compose exec mysql mysql -u root -p

# 检查连接日志
docker-compose logs mysql | grep "Access denied"
```

**解决方案：**
1. 检查 `DB_PASSWORD` 是否与 MySQL 初始化密码一致
2. 确认数据库用户权限配置正确
3. 检查 `docker-compose.yml` 中的数据库连接配置

### 数据迁移失败

**排查步骤：**
```bash
# 手动执行迁移
docker-compose exec [service] sh -c "java -jar app.jar migrate"

# 检查迁移脚本
docker-compose exec mysql ls -la /docker-entrypoint-initdb.d/
```

---

## 网络连接问题

### 前端无法连接后端

**症状：** 前端显示网络错误或超时

**排查步骤：**
```bash
# 检查网关是否正常
curl http://localhost:8088/actuator/health

# 检查CORS配置
# 确认 .env 中的 CORS_ORIGINS 包含前端地址

# 检查浏览器控制台
# F12 打开开发者工具查看 Network 标签
```

**解决方案：**
1. 确认网关服务正常运行
2. 检查 `CORS_ORIGINS` 配置
3. 检查防火墙规则

### 服务间无法通信

**排查步骤：**
```bash
# 测试服务间连通性
docker-compose exec [service-a] ping [service-b]

# 检查网络配置
docker network inspect trae_default
```

**解决方案：**
1. 确保所有服务在同一Docker网络中
2. 检查服务名称是否正确（区分大小写）
3. 检查端口映射是否正确

---

## 性能问题

### API响应缓慢

**排查步骤：**
```bash
# 检查资源使用
docker stats

# 查看慢查询日志
docker-compose logs [service] | grep "Slow query"
```

**解决方案：**
1. 增加服务实例：`docker-compose up -d --scale [service]=3`
2. 优化数据库索引
3. 启用Redis缓存

### 内存溢出 (OOM)

**排查步骤：**
```bash
# 查看容器内存使用
docker stats --no-stream

# 检查JVM堆内存配置
docker-compose exec [service] jmap -heap [pid]
```

**解决方案：**
1. 增加容器内存限制
2. 优化JVM参数：
   ```yaml
   environment:
     - JAVA_OPTS=-Xmx2g -Xms512m
   ```

### 算法调用超时

**症状：** 路径规划或数据同化请求超时

**排查步骤：**
```bash
# 检查Python Executor日志
docker-compose logs uav-platform | grep "timeout"

# 检查资源使用
docker stats

# 增加超时配置
# 修改 .env 中的 UAV_PYTHON_TIMEOUT
```

**解决方案：**
1. 增加Python执行超时时间
2. 优化算法参数
3. 增加计算资源

---

## 算法问题

### 路径规划失败

**排查步骤：**
```bash
# 查看路径规划服务日志
docker-compose logs path-planning-service

# 检查气象数据
curl http://localhost:8082/api/forecast?lat=39.9&lng=116.4
```

**常见原因：**
| 原因 | 解决方案 |
|------|----------|
| 无有效气象数据 | 上传WRF气象数据 |
| 气象条件不满足 | 调整气象约束条件 |
| 算法超时 | 增加超时配置 |

### 数据同化失败

**排查步骤：**
```bash
# 查看同化服务日志
docker-compose logs data-assimilation-service

# 检查观测数据格式
cat data/observations.json | python -m json.tool
```

**解决方案：**
1. 确保观测数据格式正确
2. 检查观测数据时间范围
3. 验证背景场数据可用性

---

## 监控与诊断

### 查看Prometheus指标

```bash
# Prometheus UI
http://localhost:9090

# 检查服务指标
curl http://localhost:9090/api/v1/query?query=up
```

### 查看Grafana仪表板

```bash
# Grafana UI
http://localhost:3000
# 默认账号: admin/admin
```

### 查看Jaeger链路追踪

```bash
# Jaeger UI
http://localhost:16686
```

---

## 获取帮助

如果问题仍未解决：

1. 查看 [改进建议](../improvement_suggestions.md)
2. 查看 [常见问题FAQ](../README.md)
3. 提交 Issue 到 [GitHub](https://github.com/602420232-dotcom/weather/issues)

---

> **最后更新**: 2026-05-08  
> **版本**: 2.1 
> **维护者**: DITHIOTHREITOL
