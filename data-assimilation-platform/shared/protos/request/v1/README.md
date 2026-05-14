# request/v1

Protocol Buffers 请求协议定义（v1），定义同化请求和批量请求的数据结构。

## 主要文件

| 文件 | 说明 |
|------|------|
| `assimilation.proto` | 同化请求定义：`AssimilationRequest`（含算法选择、背景场、观测列表、配置）、`Observation`、`AssimilationConfig` |
| `batch.proto` | 批量同化请求定义：`BatchRequest`，支持一次提交多个同化任务 |

## Proto 结构

```protobuf
message AssimilationRequest {
  string job_id = 1;
  string algorithm = 2;                    // "3dvar" | "4dvar" | "enkf" | "hybrid"
  GridDefinition background_grid = 3;
  map<string, bytes> background_variables = 4;
  repeated Observation observations = 5;
  AssimilationConfig config = 6;
}

message Observation {
  GeoPoint location = 1;
  string variable = 2;
  double value = 3;
  double error = 4;
  Timestamp time = 5;
}
```

## 引用

```
import "common/v1/types.proto";
import "common/v1/geometry.proto";
import "common/v1/metadata.proto";
```

## Java 包路径

```
com.uav.bayesian.request.v1
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
