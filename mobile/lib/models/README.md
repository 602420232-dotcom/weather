# models

数据模型层，定义应用中各业务实体的 Dart 数据类。每个模型均支持 JSON 序列化/反序列化，部分模型提供了 `copyWith`、计算属性等便利方法。

## 关键文件

| 文件 | 说明 |
|------|------|
| `drone.dart` | 无人机模型，包含名称、型号、载荷、续航、电池、在线/可用状态判断 |
| `task.dart` | 任务模型 + `TaskConstraints` + `Waypoint`，含航点列表、优先级、约束条件 |
| `waypoint.dart` | 航点模型，包含经纬度、顺序、可选高度，支持 `LatLng` 转换 |
| `weather_data.dart` | 气象数据模型，含风速/风向/温度/湿度/能见度等字段，内置风险等级计算 |
| `user.dart` | 用户模型 + `LoginResponse`，含角色判断 `isAdmin` |
| `path_plan.dart` | 路径规划结果模型，含算法、状态、航点列表、距离、耗时估算 |
| `data_source.dart` | 数据源模型，含类型标签（地面站/浮标/卫星/气象站/雷达） |
| `system_status.dart` | 系统状态、服务状态、算法性能模型 |

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
