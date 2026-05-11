# 无人机气象驱动智能路径规划系统- 移动客户?
基于 Flutter 开发的跨平台移动应用支持 iOS ?Android?
## 功能特性
-  **系统驾驶?* - 实时概览系统状态任务统计气象预报- ?**路径规划** - 交互式地图展示VRPTW/A*/DWA 三层路径规划
- ?**气象数据** - WRF气象数据可视化风速风向热力图
-  **任务管理** - 无人机任务创建分配执行跟踪-  **无人机管理* - 无人机注册状态监控电量管理-  **系统监控** - CPU/内存/磁盘资源监控服务状态巡检
-  **边云协同** - 离线评估边缘任务队列联邦学习- ?**智能驾驶?* - 态势感知综合面板

## 技术栈

| 技?| 用途|
|------|------|
| Flutter/Dart | 跨平?UI 框架 |
| Riverpod | 状态管理|
| GoRouter | 路由导航 |
| Dio | 网络请求 |
| flutter_map + OpenStreetMap | 地图服务 |
| fl_chart | 数据可可视化|
| Hive + SharedPreferences | 本地存储 |
| flutter_secure_storage | 安全存储 |

## 快速开始
### 环境要求

- Flutter SDK >= 3.2.0
- Dart SDK >= 3.2.0
- Android Studio / Xcode
- 设备/模拟器iOS 14+ / Android 5.0+?
### 安装

```bash
cd uav-mobile-app
flutter pub get
```

### 运行

```bash
# Android
flutter run -d android

# iOS
flutter run -d ios

# Web (实验?
flutter run -d chrome
```

### 测试

```bash
flutter test
```

## 项目结构

```
uav-mobile-app/
 lib/
-   main.dart                       # 应用入口
-   app.dart                        # 路由配置
-   config/
-    app_config.dart             # 应用配置/主题
-   core/
-    network/
-   ?   api_client.dart         # HTTP 客户端  ?  ?   api_interceptor.dart    # 认证拦截器  ?  ?   api_exception.dart      # 异常定义
-    storage/
-   ?   local_storage.dart      # SharedPreferences
-   ?   secure_storage.dart     # 安全存储
-    utils/
-        logger.dart             # 日志工具
-        debouncer.dart          # 防抖工具
-   models/
-    user.dart                   # 用户模型
-    drone.dart                  # 无人机模型  ?   task.dart                   # 任务模型
-    weather_data.dart           # 气象数据模型
-    path_plan.dart              # 路径规划模型
-    data_source.dart            # 数据源模型  ?   system_status.dart          # 系统状态模型   services/
-    auth_service.dart           # 认证服务
-    drone_service.dart          # 无人机服务  ?   task_service.dart           # 任务服务
-    weather_service.dart        # 气象服务
-    planning_service.dart       # 路径规划服务
-    data_source_service.dart    # 数据源服务  ?   monitoring_service.dart     # 监控服务
-    edge_coordinator_service.dart # 边云协同服务
-    offline_manager.dart        # 离线管理
-   providers/
-    app_providers.dart          # Riverpod 状态管理   pages/
-    main_shell.dart             # 导航框架
-    login/                      # 登录页  ?   home/                       # 首页
-    planning/                   # 路径规划
-    weather/                    # 气象数据
-    tasks/                      # 任务管理
-    drones/                     # 无人机管理  ?   history/                    # 历史记录
-    data_sources/               # 数据源管理  ?   monitoring/                 # 系统监控
-    cockpit/                    # 智能驾驶舱  ?   edge/                       # 边云协同
-    settings/                   # 设置
-   widgets/
-       common/
-           app_widgets.dart        # 通用组件
 test/
-   models_test.dart                # 模型和服务测试 assets/
-   config/
-       default_config.json         # 默认配置
 .github/workflows/
-   ci-cd.yml                       # CI/CD 配置
 pubspec.yaml
 README.md
```

## 后端API对接

应用默认连接?`http://localhost:8088` ?API 网关集成以下微服务接口?
| 服务 | 端点 | 用途|
|------|------|------|
| Platform | `/api/v1/*` | 用户认证无人机/任务管理 |
| WRF Processor | `/api/wrf/*` | WRF 气象数据解析 |
| Meteor Forecast | `/api/forecast/*` | 气象预测与订?|
| Path Planning | `/api/planning/*` | VRPTW/A*/DWA 路径规划 |
| Data Assimilation | `/api/assimilation/*` | 贝叶斯数据同?|
| Weather Collector | `/api/weather/*` | 气象数据采集融合 |
| Edge Coordinator | `:8000/*` | 边云协同任务 |

## 发布

```bash
# Android APK
flutter build apk --release
# 输出: build/app/outputs/flutter-apk/app-release.apk

# Android App Bundle
flutter build appbundle --release

# iOS
flutter build ios --release
```

