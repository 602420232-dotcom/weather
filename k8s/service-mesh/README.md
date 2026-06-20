# service-mesh

Istio 服务网格配置，为 UAV 平台提供流量管理、安全通信 (mTLS)、弹性治理和可观测性 (链路追踪)。

## 文件说明

| 文件 | 说明 |
|------|------|
| `istio.yml` | Istio 安装 + VirtualService + DestinationRule + Gateway |
| `governance.yml` | 流量治理: VirtualService / DestinationRule / EnvoyFilter / Telemetry / PeerAuthentication |

## 组件矩阵

### Istio 安装配置 (`istio.yml`)

| 配置项 | 值 | 说明 |
|-------|-----|------|
| Profile | `default` | 默认 Istio 配置模板 |
| Egress Gateway | enabled | 出口流量网关 |
| Ingress Gateway | enabled | 入口端口: 80/443 |
| 链路追踪 | enabled | Jaeger 采样率 100% |
| 访问日志 | JSON 格式 | 输出到 stdout |

### 流量治理 (`governance.yml`)

#### VirtualService — 智能路由

| 服务 | 路由规则 | 说明 |
|------|---------|------|
| `uav-platform` | `/api/v1` → uav-platform (100%) | 平台服务 API |
| | `/api/planning` → path-planning v1 (90%) + v2 (10%) | 路径规划金丝雀 |
| | 重试: 3 次, 超时: 10s | 自动重试 + CORS |

#### DestinationRule — 弹性策略

| 策略 | 配置 | 说明 |
|------|------|------|
| 负载均衡 | `ROUND_ROBIN` | 轮询分发 |
| 连接池 | TCP 100 / HTTP2 100 | 最大连接数 |
| 异常检测 | 连续 5xx → 隔离 30s | 故障实例排除 |
| 重试策略 | 3 次重试, 超时 3s | 自动恢复 |

#### EnvoyFilter — 熔断器

| 参数 | 值 |
|------|:--:|
| 最大连接数 | 500 |
| 最大等待请求 | 100 |
| 最大请求数 | 200 |
| 最大重试次数 | 5 |

#### mTLS 双向认证

```yaml
mtls:
  mode: STRICT  # 严格 mTLS 模式
```

所有服务间通信强制使用 mTLS 加密。

#### Telemetry — 链路追踪

| 配置 | 值 |
|------|-----|
| 采样率 | 100% |
| 追踪后端 | Jaeger |
| 自定义标签 | `environment: production`, `service_version` |

## 快速开始

### 安装 Istio

```bash
# 安装 Istio
istioctl install -f deployments/service-mesh/istio.yml

# 启用 sidecar 注入
kubectl label namespace uav-platform istio-injection=enabled
```

### 应用服务网格配置

```bash
kubectl apply -f deployments/service-mesh/governance.yml
```

### 验证

```bash
# 检查 Pod sidecar 注入
kubectl get pods -n uav-platform -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[*].name}{"\n"}{end}'

# 查看 VirtualService
kubectl get virtualservice -n uav-platform

# 查看 DestinationRule
kubectl get destinationrule -n uav-platform

# 查看 PeerAuthentication
kubectl get peerauthentication -n uav-platform
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
