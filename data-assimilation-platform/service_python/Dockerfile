FROM python:3.11-slim
RUN groupadd -r appuser && useradd -r -g appuser -m -s /sbin/nologin appuser
WORKDIR /app
RUN apt-get update && apt-get install -y \
    build-essential \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
RUN chown -R appuser:appuser /app
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app
EXPOSE 8000
USER appuser
HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1
CMD ["python", "src/main.py"]
