# service/v2

Protocol Buffers 服务定义（v2），在 v1 基础上扩展 gRPC 服务接口，支持流式处理。

## 主要文件

| 文件 | 说明 |
|------|------|
| `assimilation_service.proto` | gRPC 同化服务 v2 定义，扩展 v1 的 RPC 接口方法 |

## 对比 v1

| RPC 方法 | v1 | v2 |
|---------|-----|-----|
| Assimilate | ✓ | ✓ |
| BatchAssimilate | ✓ (stream response) | ✓ |
| ComputeVariance | ✓ | ✓ |
| GetStatus | ✓ | ✓ |
| StreamAssimilate | — | ✓ (新增：双向流) |

## Java 包路径

```
com.uav.bayesian.service.v2
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
