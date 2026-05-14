# request/v2

Protocol Buffers 请求协议定义（v2），在 v1 基础上扩展支持流式请求等新场景。

## 主要文件

| 文件 | 说明 |
|------|------|
| `assimilation.proto` | 同化请求 v2 定义，扩展 v1 结构 |
| `streaming.proto` | 流式同化请求定义，支持持续数据流输入 |

## 对比 v1

| 特性 | v1 | v2 |
|------|-----|-----|
| 同化请求 | 单一任务 | 增强参数 + 流式支持 |
| 批量请求 | BatchRequest | — |
| 流式请求 | — | StreamingRequest（新增） |

## Java 包路径

```
com.uav.bayesian.request.v2
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
