# 基于NVIDIA CUDA基础镜像
FROM nvidia/cuda:11.8.0-cudnn8-devel-ubuntu20.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-dev \
    build-essential \
    git \
    wget \
    && rm -rf /var/lib/apt/lists/*

RUN pip3 install --upgrade pip

WORKDIR /app

COPY . /app

RUN pip3 install -e .[dev,prod] && \
    groupadd -r appuser && useradd -r -g appuser -m -s /sbin/nologin appuser && \
    chown -R appuser:appuser /app

ENV PYTHONPATH=/app

EXPOSE 8000
USER appuser

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD python3 -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1

CMD ["python3", "-m", "bayesian_assimilation.api.web"]