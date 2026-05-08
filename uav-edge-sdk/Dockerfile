FROM python:3.11-slim AS builder
LABEL stage="builder"

WORKDIR /build

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential cmake git \
    && rm -rf /var/lib/apt/lists/*

COPY CMakeLists.txt .
COPY src/ src/
COPY include/ include/
COPY requirements.txt .

RUN pip install --no-cache-dir pybind11

RUN mkdir -p build && cd build && \
    cmake .. -DCMAKE_BUILD_TYPE=Release && \
    make -j$(nproc) && \
    make install

FROM python:3.11-slim
LABEL service="uav-edge-sdk"
LABEL version="1.0.0"

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/python/*.so /app/edge_sdk/
COPY --from=builder /build/python/*.pyd /app/edge_sdk/ 2>/dev/null || true
COPY python/ /app/edge_sdk/
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY src/edge_sdk.py /app/
COPY README.md /app/

ENV PYTHONPATH="/app/edge_sdk:${PYTHONPATH}"

EXPOSE 8765

HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
    CMD python -c "from edge_sdk import EdgeSDK; exit(0)" || exit 1

CMD ["python", "-c", "from edge_sdk import EdgeSDK; print('UAV Edge SDK ready')"]
