# 贝叶斯同化系统 API Dockerfile
FROM python:3.11-slim

# 设置工作目录
WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    git \
    && rm -rf /var/lib/apt/lists/*

# 复制项目文件
COPY src/ ./src/

# 设置 Python 路径
ENV PYTHONPATH=/app/src
ENV PYTHONUNBUFFERED=1

# 安装 Python 依赖
RUN pip install --no-cache-dir \
    numpy>=1.24.0 \
    scipy>=1.11.0 \
    netCDF4>=1.6.0 \
    h5py>=3.9.0 \
    pandas>=2.0.0 \
    flask>=3.0.0 \
    flask-cors>=4.0.0 \
    fastapi>=0.100.0 \
    uvicorn>=0.23.0 \
    pydantic>=2.0.0

# 安装项目（以可编辑模式）
RUN pip install --no-cache-dir -e ./src

# 暴露端口
EXPOSE 5000 8000

# 启动脚本
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
CMD ["api"]
