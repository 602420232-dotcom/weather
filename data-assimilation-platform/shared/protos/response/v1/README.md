# response/v1

Protocol Buffers 响应协议定义（v1），定义同化计算返回结果的数据结构。

## 主要文件

| 文件 | 说明 |
|------|------|
| `assimilation.proto` | 同化响应定义：`AssimilationResponse`（job_id、分析变量、方差场、指标）、`AssimilationMetrics` |
| `status.proto` | 状态查询响应定义：`StatusResponse`（服务状态、队列大小、运行任务数） |
| `stream.proto` | 流式响应定义：`StreamResponse`（批量/流式结果推送） |

## Proto 结构

```protobuf
message AssimilationResponse {
  string job_id = 1;
  bool success = 2;
  string message = 3;
  map<string, bytes> analysis_variables = 4;
  common.v1.VarianceField variance = 5;
  AssimilationMetrics metrics = 6;
}

message AssimilationMetrics {
  int32 iterations = 1;
  double final_cost = 2;
  double reduction_rate = 3;
  double elapsed_seconds = 4;
}
```

## 引用

```
import "common/v1/geometry.proto";
import "common/v1/variance_field.proto";
```

## Java 包路径

```
com.uav.bayesian.response.v1
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
