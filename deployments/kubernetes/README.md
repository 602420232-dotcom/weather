# kubernetes

UAV 路径规划系统 Kubernetes 生产部署配置，包含所有微服务的 Deployment、Service、HPA、PVC 和 Ingress 定义。

## 文件说明

| 文件 | 说明 |
|------|------|
| `namespace.yml` | 命名空间定义 (`uav-platform`) |
| `secrets.yml` | Kubernetes Secret 敏感信息 |
| `persistent-volumes.yml` | 持久卷声明 (MySQL/Redis/WRF/模型) |
| `database-services.yml` | 数据库服务 (MySQL + Redis) |
| `nginx-ingress.yml` | Nginx Ingress 入口控制器 |
| `api-gateway.yml` | API 网关服务部署 |
| `uav-platform-service.yml` | 无人机平台主服务部署 |
| `uav-platform.yml` | 无人机平台 Deployment |
| `wrf-processor-service.yml` | WRF 处理器服务部署 |
| `wrf-processor.yml` | WRF 处理器 Deployment |
| `meteor-forecast-service.yml` | 气象预测服务部署 |
| `meteor-forecast.yml` | 气象预测 Deployment |
| `path-planning-service.yml` | 路径规划服务部署 |
| `path-planning.yml` | 路径规划 Deployment |
| `data-assimilation-service.yml` | 数据同化服务部署 |
| `data-assimilation.yml` | 数据同化 Deployment |
| `uav-weather-collector.yml` | 气象采集服务部署 |
| `edge-cloud-coordinator.yml` | 边缘云协调器部署 |
| `frontend-vue.yml` | Vue 前端部署 |
| `autoscaling.yml` | 水平自动扩缩容 (HPA) |
| `hpa.yml` | 额外 HPA 配置 |
| `hpa-supplement.yml` | HPA 补充配置 |
| `monitoring.yml` | Prometheus 监控组件 |
| `sonarqube.yml` | SonarQube 代码质量分析 |
| `backup-cronjob.yml` | 数据库备份 CronJob |
| `deploy.sh` | 一键部署脚本 |

## 持久卷 (PersistentVolumeClaim)

| PVC 名称 | 用途 | 大小 |
|---------|------|:----:|
| `mysql-data-pvc` | MySQL 数据存储 | 10Gi |
| `redis-data-pvc` | Redis 数据存储 | 2Gi |
| `wrf-data-pvc` | WRF 处理中间数据 | 50Gi |
| `output-data-pvc` | 输出结果存储 | 20Gi |
| `model-data-pvc` | AI 模型文件存储 | 30Gi |
| `forecast-data-pvc` | 气象预测数据 | 20Gi |
| `config-data-pvc` | 配置文件存储 | 1Gi |

## 自动扩缩容 (HPA)

| 服务 | minReplicas | maxReplicas | CPU 阈值 | Memory 阈值 |
|------|:----------:|:----------:|:------:|:---------:|
| uav-platform | 2 | 10 | 70% | 80% |
| path-planning | 2 | 8 | 60% | 70% |
| frontend-vue | 2 | 12 | 50% | 60% |

## 快速开始

### 前置条件

- Kubernetes 集群 (v1.25+)
- kubectl 已配置集群访问
- 已创建 StorageClass `standard`

### 一键部署

```bash
# 进入部署目录
cd deployments/kubernetes

# 执行部署脚本
bash deploy.sh
```

### 分步部署

```bash
# 1. 创建命名空间
kubectl apply -f namespace.yml

# 2. 创建 Secret
kubectl apply -f secrets.yml

# 3. 创建持久卷
kubectl apply -f persistent-volumes.yml

# 4. 部署数据库
kubectl apply -f database-services.yml

# 5. 部署后端服务
kubectl apply -f wrf-processor-service.yml
kubectl apply -f meteor-forecast-service.yml
kubectl apply -f path-planning-service.yml
kubectl apply -f uav-platform-service.yml

# 6. 部署前端
kubectl apply -f frontend-vue.yml

# 7. 部署自动扩展
kubectl apply -f autoscaling.yml

# 8. 应用所有配置
kubectl apply -f kubernetes/
```

### 验证部署

```bash
# 查看所有资源
kubectl get all -n uav-platform

# 查看 HPA 状态
kubectl get hpa -n uav-platform

# 查看 PVC 状态
kubectl get pvc -n uav-platform

# 查看 Pod 日志
kubectl logs -n uav-platform <pod-name>
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
