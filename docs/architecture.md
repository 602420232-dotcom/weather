# 架构设计文档

## 模式边界定义

本项目采*微服务分层架*模块边界清晰定义如下

### 模块分层

```
?
? common-dependencies  统一依赖管理BOM?        ?
? common-utils          共享工具库安全/审计/执行器? api-gateway           网关(端口8088)          ?
?
? uav-platform-service  编排(端口8080)          ?
? 职责服务编排数据源管理?
? wrf-processor-service  领域(端口8081)         ?
? meteor-forecast-service 领域(端口8082)        ?
? path-planning-service   领域(端口8083)        ?
? data-assimilation-service 领域(端口8084)      ?
? uav-weather-collector   领域(端口8086)        ?
?
? backend-spring        独立服务 (端口8089)        ?
? 职责路径规划系统后端含认授权/历史管理?
?
```

### 模块边界定义

| 模块 | 端口 | 职责边界 |
|------|:---:|---------|
| **common-dependencies** | N/A | 统一BOM集中管理所有依赖版本各子模块引入此POM即可获得全部通用依赖 |
| **common-utils** | N/A | 共享工具库PythonExecutorJWT过滤器SecurityAuditor审计NacosConfigRefresherCsrfOriginFilter |
| **api-gateway** | 8088 | API网关路由转发限流熔|
| **uav-platform-service** | 8080 | 平台编排服务间编排调用链数据源CRUD实时数据获取*不含**独立认证/路径规划逻辑 |
| **backend-spring** | 8089 | 独立路径规划系统用户认证授权独立路径规划算法调用路径历史管理依赖common模块但不参与微服务编|
| **领域服务** | 8081-8086 | 各领域微服务处理各自领域的核心算法和业务逻辑 |

### backend-spring vs uav-platform-service 职责区分

| 职责 | backend-spring | uav-platform-service |
|------|:---:|:---:|
| 用户认证 | ?AuthController + JWT + BCrypt | ?使用common基础认证 |
| 角色权限 | ?RBACADMIN/DISPATCHER/OPERATOR/USER?| ?仅基础认证 |
| 路径规划 | ?PathPlanningController直接Python调用| ?PlatformController微服务编排|
| CSRF保护 | ?CookieCsrfTokenRepository | ?JWT无状态模|
| 数据源管| ✅ | ?DataSourceController + RealDataSourceController |
| Nacos注册 | ?独立部署 | ?Nacos服务发现 |
| 链路追踪 | ✅ | ?SkyWalking |

### 架构决策记录

**ADR-001**: backend-spring 为独立路径规划系统使用端口8089非8080不与 uav-platform-service 存在端口冲突两个模块不共享Controller/Service由 gateway 根据路由区分访问目标**ADR-002**: 所有公共工具全全组件统一收归 common-utilsSecurityAuditor 为唯一审计实现各模块通过 SecurityAuditConfig 委托调用**ADR-003**: common-dependencies 为BOM型POM集中管理所有通用依赖版本子模块引入 common-dependencies 即可获得全部标准化依赖无需在各自pom中重复声明---

> **最后更新*: 2026-05-09  
> **版本**: 2.1  
> **维护者*: DITHIOTHREITOL

