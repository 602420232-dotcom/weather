# services

服务层，封装与后端 API 交互的业务逻辑。每个服务类负责一个业务领域，通过 `ApiClient` 发送 HTTP 请求，将原始响应数据转换为领域模型。

## 关键文件

| 文件 | 说明 |
|------|------|
| `auth_service.dart` | 认证服务，处理登录、登出、Token 刷新、当前用户获取 |
| `drone_service.dart` | 无人机管理服务，CRUD 操作 `/api/v1/drones` |
| `task_service.dart` | 任务管理服务，CRUD 操作及路径规划请求 `/api/v1/tasks` |
| `weather_service.dart` | 气象数据服务，获取无人机气象、融合数据、预警信息 |
| `planning_service.dart` | 路径规划服务，支持 VRPTW / A* / DWA / 全链路规划及气象预测订正 |
| `data_source_service.dart` | 数据源管理服务，CRUD 操作及实时数据获取（地面站/浮标） |
| `monitoring_service.dart` | 系统监控服务，健康检查、断路器状态、服务在线检测、算法性能 |
| `edge_coordinator_service.dart` | 边缘协同服务，任务提交/状态查询/取消、气象风险评估 |
| `offline_manager.dart` | 离线管理器，网络状态监听、数据缓存/读取/清除 |

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
