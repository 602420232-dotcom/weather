# service/v1

Protocol Buffers gRPC 服务定义（v1），定义同化计算的所有 RPC 接口方法。

## 主要文件

| 文件 | 说明 |
|------|------|
| `assimilation_service.proto` | gRPC 同化服务定义：`AssimilationService` 及四个 RPC 方法 |

## RPC 接口

```protobuf
service AssimilationService {
  // 单一同化任务
  rpc Assimilate(request.v1.AssimilationRequest) returns (response.v1.AssimilationResponse);

  // 批量同化（流式响应）
  rpc BatchAssimilate(request.v1.BatchRequest) returns (stream response.v1.AssimilationResponse);

  // 方差场计算
  rpc ComputeVariance(common.v1.VarianceRequest) returns (common.v1.VarianceResponse);

  // 服务状态查询
  rpc GetStatus(google.protobuf.Empty) returns (response.v1.StatusResponse);
}
```

## 引用

```
import "request/v1/assimilation.proto";
import "request/v1/batch.proto";
import "response/v1/assimilation.proto";
import "response/v1/status.proto";
import "common/v1/variance_field.proto";
```

## Java 包路径

```
com.uav.bayesian.service.v1
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
