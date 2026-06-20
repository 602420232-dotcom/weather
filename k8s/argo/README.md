# ArgoCD

ArgoCD GitOps 部署配置，实现 UAV 平台基于 Git 仓库的声明式持续交付与金丝雀发布。

## 文件说明

| 文件 | 说明 |
|------|------|
| `argocd.yml` | ArgoCD Application + Rollout 配置 |

## 配置详情

### ArgoCD Application — 主环境

| 配置项 | 值 |
|-------|-----|
| 应用名 | `uav-platform` |
| Git 仓库 | `https://github.com/uav-platform/uav-path-planning` |
| 目标版本 | `main` |
| K8s 路径 | `deployments/kubernetes` |
| 目标集群 | `https://kubernetes.default.svc` |
| 命名空间 | `uav-platform` |

### ArgoCD Application — 金丝雀环境

| 配置项 | 值 |
|-------|-----|
| 应用名 | `uav-platform-canary` |
| 目标版本 | `canary` |
| 命名空间 | `uav-platform-canary` |

### 同步策略

| 策略 | 值 | 说明 |
|------|:--:|------|
| `prune` | `true` | 自动删除 Git 中移除的资源 |
| `selfHeal` | `true` | 自动修复手动变更 |
| `CreateNamespace` | `true` | 自动创建命名空间 |
| `PrunePropagationPolicy` | `foreground` | 级联删除 |
| `PruneLast` | `true` | 最后删除被 prune 的资源 |

### Argo Rollout — 金丝雀发布 (path-planning)

| 步骤 | 权重 | 操作 |
|:--:|:---:|------|
| 1 | 10% | 发布到 10% 流量 |
| — | — | 暂停 60s |
| 2 | 30% | 扩展到 30% 流量 |
| — | — | 暂停 60s |
| 3 | 60% | 扩展到 60% 流量 |
| — | — | 暂停 60s |
| 4 | 100% | 全量发布 |

### 资源限制 (Rollout)

| 资源 | Limit | Request |
|------|:-----:|:------:|
| CPU | 500m | 200m |
| Memory | 512Mi | 256Mi |

### 健康检查 (Rollout)

| 探针 | 路径 | 初始延迟 | 周期 |
|------|------|:------:|:---:|
| Liveness | `/actuator/health` | 30s | 10s |
| Readiness | `/actuator/health` | 20s | 5s |

## 快速开始

### 安装 ArgoCD

```bash
# 安装 ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

### 应用 GitOps 配置

```bash
kubectl apply -f deployments/argo/argocd.yml
```

### 访问 ArgoCD

```bash
# 获取 admin 密码
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d

# 端口转发
kubectl port-forward svc/argocd-server -n argocd 8080:443

# 访问
open https://localhost:8080
```

### 查看同步状态

```bash
# 查看 Application
argocd app get uav-platform

# 查看 Rollout
kubectl argo rollouts get rollout path-planning -n uav-platform

# 监控金丝雀发布
kubectl argo rollouts get rollout path-planning -n uav-platform --watch
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
