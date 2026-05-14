# common/v1

Protocol Buffers 通用定义（v1），被请求/响应/服务协议共同引用的基础数据类型。

## 主要文件

| 文件 | 说明 |
|------|------|
| `geometry.proto` | 空间几何定义：`BoundingBox`（边界框）、`GridDefinition`（网格经纬度/层级）、`Polygon`（多边形）、`Circle`（圆形区域） |
| `metadata.proto` | 元数据定义：任务信息、时间戳、来源描述等 |
| `types.proto` | 基础类型定义：`GeoPoint`（经纬高坐标点）、枚举类型等 |
| `variance_field.proto` | 方差场 Proto 定义：`VarianceField`、`VarianceRequest`、`VarianceResponse` |

## 引用关系

```
common/v1/
    ├── types.proto          ← 被 geometry.proto 引用
    ├── geometry.proto       ← 被 request/、response/ 引用
    ├── metadata.proto       ← 被 request/ 引用
    └── variance_field.proto ← 被 response/、service/ 引用
```

## Java 包路径

```
com.uav.bayesian.common.v1
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
