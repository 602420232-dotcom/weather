# ==============================================
# 统一构建脚本 (Makefile)
# 支持 Windows/Linux/macOS
# 位于 scripts/ 目录，从项目根目录调用
# ==============================================

.PHONY: help maven-deps maven-clean build test test-python test-all docker-up docker-down clean help

PROJECT_ROOT := $(realpath $(dir $(lastword $(MAKEFILE_LIST)))/..)

help:
	@echo "无人机路径规划系统 - 构建脚本"
	@echo "用法:"
	@echo "  make maven-deps      重新下载 Maven 依赖"
	@echo "  make maven-clean     清理 Maven 构建产物"
	@echo "  make build           构建所有 Java 服务"
	@echo "  make test            运行 Java 测试"
	@echo "  make test-python     运行 Python 测试"
	@echo "  make test-all        运行所有测试"
	@echo "  make docker-up       启动 Docker Compose"
	@echo "  make docker-down     停止 Docker Compose"
	@echo "  make clean           清理构建产物"
	@echo ""

# ===== Maven 服务列表 =====
MAVEN_SERVICES = api-gateway wrf-processor-service meteor-forecast-service \
                 path-planning-service data-assimilation-service \
                 uav-platform-service uav-weather-collector \
                 uav-path-planning-system/backend-spring

maven-deps:
	@echo "[INFO] Maven 依赖重新下载..."
	cd "$(PROJECT_ROOT)" && mvn clean install -U -DskipTests -B
	@echo "[INFO] 完成！"

maven-clean:
	@echo "[INFO] 清理 Maven 构建产物..."
	cd "$(PROJECT_ROOT)" && mvn clean -B
	@echo "[INFO] 完成！"

build:
	@echo "[INFO] 构建所有服务..."
	cd "$(PROJECT_ROOT)" && mvn clean package -DskipTests -B
	@echo "[INFO] 构建完成！"

test:
	@echo "[INFO] 运行 Java 测试..."
	cd "$(PROJECT_ROOT)" && mvn test -B
	@echo "[INFO] 测试完成！"

test-python:
	@echo "[INFO] 运行 Python 测试..."
	cd "$(PROJECT_ROOT)" && pip install pytest numpy -q
	cd "$(PROJECT_ROOT)" && python -m pytest tests/ -v --tb=short
	@echo "[INFO] Python 测试完成！"

test-all: test test-python
	@echo "[INFO] 所有测试完成！"

docker-up:
	@echo "[INFO] 启动 Docker Compose..."
	cd "$(PROJECT_ROOT)" && docker compose up -d --build
	@echo "[INFO] 启动完成！"

docker-down:
	@echo "[INFO] 停止 Docker Compose..."
	cd "$(PROJECT_ROOT)" && docker compose down
	@echo "[INFO] 停止完成！"

docker-logs:
	cd "$(PROJECT_ROOT)" && docker compose logs -f

clean: maven-clean
	@echo "[INFO] 清理 Python 产物..."
	cd "$(PROJECT_ROOT)" && rm -rf */target */build **/__pycache__ **/*.pyc
	@echo "[INFO] 清理完成！"
