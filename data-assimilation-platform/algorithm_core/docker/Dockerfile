FROM python:3.11-slim
RUN groupadd -r appuser && useradd -r -g appuser -m -s /sbin/nologin appuser
WORKDIR /app
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    git \
    && rm -rf /var/lib/apt/lists/*
COPY src/ ./src/
ENV PYTHONPATH=/app/src
ENV PYTHONUNBUFFERED=1
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
RUN pip install --no-cache-dir -e ./src
RUN chown -R appuser:appuser /app
EXPOSE 5000 8000
USER appuser
HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
CMD ["api"]
