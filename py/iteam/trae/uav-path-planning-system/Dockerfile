# 基础镜像
FROM openjdk:11-jdk-slim

# 设置工作目录
WORKDIR /app

# 安装Python和依赖
RUN apt-get update && apt-get install -y python3 python3-pip python3-dev gcc g++

# 复制项目文件
COPY . .

# 安装Python依赖
RUN pip3 install -r requirements.txt

# 构建SpringBoot应用
RUN ./mvnw clean package -DskipTests

# 暴露端口
EXPOSE 8080

# 启动应用
CMD ["java", "-jar", "backend-spring/target/uav-path-planning-0.0.1-SNAPSHOT.jar"]