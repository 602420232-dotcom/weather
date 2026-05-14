# response/v2

Protocol Buffers 响应协议定义（v2），扩展 v1 响应结构，支持流式响应等新特性。

## 主要文件

| 文件 | 说明 |
|------|------|
| `assimilation.proto` | 同化响应 v2 定义，扩展 v1 的响应消息结构 |

## 对比 v1

| 特性 | v1 | v2 |
|------|-----|-----|
| AssimilationResponse | ✓ | ✓（增强） |
| StatusResponse | ✓ | —（合并入 response） |
| StreamResponse | ✓ | ✓（增强） |
| 流式元数据 | — | ✓（新增） |

## Java 包路径

```
com.uav.bayesian.response.v2
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
