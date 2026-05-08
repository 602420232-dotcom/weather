FROM python:3.11-slim
LABEL service="uav-edge-device"
LABEL version="1.0.0"

RUN groupadd -r appuser && useradd -r -g appuser -m -s /sbin/nologin appuser

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc g++ cmake \
    libgl1-mesa-glx libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements-edge.txt .
RUN pip install --no-cache-dir -r requirements-edge.txt

COPY . .
RUN chown -R appuser:appuser /app

EXPOSE 8080
EXPOSE 8765
USER appuser

HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8080/health')" || exit 1

CMD ["python", "-m", "api", "--host", "0.0.0.0", "--port", "8080"]
