# 故障排除指南

## 常见问题

### 1. 服务无法启动

**症状**: 微服务启动失败，日志显示连接超时

**解决方案**:
1. 检查 Nacos 是否正常运行: `curl http://localhost:8848/nacos`
2. 检查 MySQL 连接: `mysql -h localhost -P 3306 -u root -p`
3. 检查 Redis 连接: `redis-cli -h localhost -p 6379 ping`
4. 确认环境变量设置: `echo $DB_HOST`

### 2. Maven 编译错误

**症状**: `mvn clean compile` 失败

**解决方案**:
1. 检查 Java 版本: `java -version` (需要 Java 17+)
2. 检查 Maven 版本: `mvn -version` (需要 3.8+)
3. 清理本地仓库: `mvn dependency:purge-local-repository`

### 3. JWT 认证失败

**症状**: API 返回 401 Unauthorized

**解决方案**:
1. 确认 JWT_SECRET 环境变量已设置
2. 检查 token 是否过期 (默认24小时)
3. 确认 token 在请求头中: `Authorization: Bearer <token>`

### 4. WRF 数据处理超时

**症状**: WRF 文件处理超时

**解决方案**:
1. 检查文件大小 (建议<500MB)
2. 确认文件格式为 NetCDF
3. 检查磁盘空间: `df -h`

### 5. Docker 容器无法启动

**症状**: docker-compose up 失败

**解决方案**:
1. 检查端口占用: `netstat -an | findstr 8080`
2. 确认 Docker 资源充足 (内存>8GB)
3. 检查 .env 文件配置

---

> **最后更新**: 2026-05-09  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL