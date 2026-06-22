# providers

基于 Riverpod 的全局状态管理层。将 Service 层封装为 Provider，并通过 `AsyncNotifier` 管理异步数据列表（加载、刷新、增删改），供 UI 层通过 `ref.watch` / `ref.read` 消费。

## 关键文件

| 文件 | 说明 |
|------|------|
| `app_providers.dart` | 全局 Provider 定义，包含认证、无人机、任务、气象、规划、数据源、监控七大模块 |

## 模块 Provider 清单

| 模块 | Service Provider | 数据 Notifier | 状态 Provider |
|------|-----------------|---------------|---------------|
| Auth | `authServiceProvider` | — | `currentUserProvider`, `isLoggedInProvider` |
| Drone | `droneServiceProvider` | `DronesNotifier` → `dronesProvider` | — |
| Task | `taskServiceProvider` | `TasksNotifier` → `tasksProvider` | — |
| Weather | `weatherServiceProvider` | — | `currentWeatherProvider`, `weatherHistoryProvider` |
| Planning | `planningServiceProvider` | — | `planningResultProvider`, `isPlanningProvider` |
| DataSource | `dataSourceServiceProvider` | `DataSourcesNotifier` → `dataSourcesProvider` | — |
| Monitoring | `monitoringServiceProvider` | — | `servicesStatusProvider`, `algorithmPerformanceProvider` |

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
