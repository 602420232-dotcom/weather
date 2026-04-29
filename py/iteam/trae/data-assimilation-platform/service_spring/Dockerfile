# 使用OpenJDK 11作为基础镜像
FROM openjdk:11-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制jar文件
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动服务
CMD ["java", "-jar", "app.jar"]
