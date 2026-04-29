# bayesian-assimilation-system/Makefile

.PHONY: help install test build proto docker clean

help:
	@echo "可用命令:"
	@echo "  make install    - 安装所有依赖"
	@echo "  make test       - 运行全部测试"
	@echo "  make proto      - 编译 Protocol Buffers"
	@echo "  make build      - 构建所有服务"
	@echo "  make docker     - 构建 Docker 镜像"
	@echo "  make deploy     - 部署到 K8s"
	@echo "  make clean      - 清理构建产物"

# 安装依赖
install:
	cd algorithm_core && pip install -e .
	cd service_python && pip install -r requirement.txt
	cd service_spring && mvn clean install -DskipTests

# 运行测试
test:
	cd algorithm_core && pytest tests/ -v --cov=bayesian_assimilation
	cd service_python && pytest tests/ -v
	cd service_spring && mvn test

# 编译 Proto（需要安装 buf 或 protoc）
proto:
	@echo "编译 v1..."
	buf generate shared/protos --path common/v1 --path request/v1 --path response/v1 --path service/v1
	@echo "编译 v2..."
	buf generate shared/protos --path common/v1 --path request/v2 --path response/v2 --path service/v2

# 构建 Docker 镜像
docker:
	docker build -t bayesian/algorithm:latest algorithm_core/
	docker build -t bayesian/python-service:latest service_python/
	docker build -t bayesian/spring-service:latest service_spring/

# 部署
deploy:
	kubectl apply -f deployments/kubernetes/

# 清理
clean:
	find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name "*.pyc" -delete
	cd service_spring && mvn clean
	rm -rf build/ dist/ *.egg-info/
