# ============================================================
# API Gateway Dockerfile - 多阶段构建优化版
# ============================================================

# ===== 第一阶段：构建 =====
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 复制pom.xml和依赖文件
COPY pom.xml .
COPY common-utils/pom.xml common-utils/
COPY api-gateway/pom.xml api-gateway/

# 下载依赖（利用Docker缓存）
RUN mvn dependency:go-offline -B || true

# 复制源代码
COPY common-utils/src common-utils/src
COPY api-gateway/src api-gateway/src

# 构建应用（跳过测试以加快构建速度）
RUN mvn package -DskipTests -B

# ===== 第二阶段：运行 =====
FROM eclipse-temurin:17-jre-alpine

# 安全：创建非root用户
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# 复制构建产物
COPY --from=builder /app/api-gateway/target/*.jar app.jar

# 设置文件权限
RUN chown -R appuser:appgroup /app

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8088

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -q --spider http://localhost:8088/actuator/health || exit 1

# JVM优化参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
