# 项目TODO清单

本文档跟踪审计中发现的所有待办事项按优先级排序## P0 - 立即修复 (Critical)

### TODO-001: 修复Grafana密码
- **状态*: 待处- **优先*: Critical
- **问题**: `deployments/kubernetes/monitoring.yml:131` 硬编码密`admin123`
- **修复方案**: 使用K8s Secret管理
- **责任*: DevOps
- **截止日期**: 2026-05-15

```yaml
# 当前 (不安装
- name: GF_SECURITY_ADMIN_PASSWORD
  value: admin123

# 修复方案
- name: GF_SECURITY_ADMIN_PASSWORD
  valueFrom:
    secretKeyRef:
      name: monitoring-secrets
      key: admin-password
```

### TODO-002: 修复ELK堆栈密码
- **状态*: 待处- **优先*: Critical
- **问题**: `deployments/monitoring/docker-compose.monitoring.yml` 密码`changeme123`
- **修复方案**: 使用强密Docker secrets
- **责任*: DevOps
- **截止日期**: 2026-05-15

---

## P1 - 本周修复 (High)

### TODO-003: 统一PythonExecutor实现
- **状态*: 待处- **优先*: High
- **问题**: 4个服务各自实现PythonExecutor代码重- **涉及文件**:
  - `data-assimilation-service/src/main/java/com/assimilation/service/utils/PythonExecutor.java`
  - `path-planning-service/src/main/java/com/path/planning/utils/PythonExecutor.java`
  - `meteor-forecast-service/src/main/java/com/meteor/forecast/utils/PythonExecutor.java`
  - `wrf-processor-service/src/main/java/com/wrf/processor/utils/PythonExecutor.java`
- **修复方案**: 统一使用 `common-utils/src/main/java/com/uav/common/utils/PythonExecutor.java`
- **责任*: 后端开- **截止日期**: 2026-05-18

### TODO-004: 统一包命名规- **状态*: 待处- **优先*: High
- **问题**: 包命名不统一部分使`com.uav.*`部分使用独立前缀
- **修复方案**: 统一`com.uav.*` 前缀
- **责任*: 架构建
- **截止日期**: 2026-05-18

### TODO-005: 气象融合权重动态配置
- **状态*: 待处- **优先*: High
- **问题**: `uav-weather-collector/.../WeatherCollectorService.java:114` 权重硬编码为 7:3
- **修复方案**: 支持动态配置权- **责任*: 后端开- **截止日期**: 2026-05-18

### TODO-006: Python脚本超时保护
- **状态*: 待处- **优先*: High
- **问题**: 多个Controller调用Python脚本缺少超时保护
- **涉及文件**:
  - WrfController.java
  - ForecastController.java
  - PlanningController.java
- **修复方案**: 添加统一的超时配置和重试机制
- **责任*: 后端开- **截止日期**: 2026-05-18

### TODO-007: 重构为Feign Client
- **状态*: 待处- **优先*: High
- **问题**: 使用RestTemplate直接调用应使用声明式Feign Client
- **涉及文件**:
  - PlatformController.java
  - WeatherCollectorCircuitBreakerService.java
- **修复方案**: 创建Feign Client接口
- **责任*: 后端开- **截止日期**: 2026-05-20

### TODO-008: Dockerfile多阶段构建
- **状态*: 待处- **优先*: High
- **问题**: 所有Dockerfile未使用多阶段构建
- **修复方案**: 采用multi-stage build减小镜像大小
- **责任*: DevOps
- **截止日期**: 2026-05-20

---

## P2 - 本月修复 (Medium)

### TODO-009: CORS配置集中管理
- **状态*: 待处- **优先*: Medium
- **问题**: CORS配置分散在多个服务
- **修复方案**: 在API网关层统一配置
- **责任*: 后端开- **截止日期**: 2026-05-25

### TODO-010: 敏感日志脱敏
- **状态*: 待处- **优先*: Medium
- **问题**: `PathPlanningController.java:30` 记录完整请求对象
- **修复方案**: 改为参数化日- **责任*: 后端开- **截止日期**: 2026-05-25

### TODO-011: 通配符导入修改
- **状态*: 待处- **优先*: Medium
- **问题**: 69处通配符导- **修复方案**: 改为明确导入
- **责任*: 后端开- **截止日期**: 2026-05-25

### TODO-012: 裸异常捕获修改
- **状态*: 待处- **优先*: Medium
- **问题**: 15处裸异常捕获
- **修复方案**: 改为具体异常类型
- **责任*: Python开- **截止日期**: 2026-05-25

### TODO-013: 补充data-assimilation-portal文档
- **状态*: 待处- **优先*: Medium
- **问题**: 缺少8094端口模块的文- **修复方案**: 补充完整文档
- **责任*: 技术文- **截止日期**: 2026-05-28

### TODO-014: Redis健康检查完- **状态*: 待处- **优先*: Medium
- **问题**: docker-compose.yml 缺少timeout参数
- **修复方案**: 添加完整健康检查配置
- **责任*: DevOps
- **截止日期**: 2026-05-28

### TODO-015: JVM参数配置
- **状态*: 待处- **优先*: Medium
- **问题**: docker-compose.yml 缺少JAVA_OPTS配置
- **修复方案**: 添加内存和GC配置
- **责任*: DevOps
- **截止日期**: 2026-05-28

### TODO-016: 熔断fallbackURI完善
- **状态*: 待处- **优先*: Medium
- **问题**: api-gateway中部分路由缺少fallbackURI
- **修复方案**: 完善降级逻辑
- **责任*: 后端开- **截止日期**: 2026-05-30

---

## P3 - 持续改进 (Low)

### TODO-017: Python类型注解自动生成
- **状态*: 待处- **优先*: Medium
- **问题**: 2000+处类型注解缺- **修复方案**: 使用 `scripts/auto_add_type_annotations.py`
- **责任*: Python开- **截止日期**: 2026-06-15

### TODO-018: Docstring补充
- **状态*: 待处- **优先*: Medium
- **问题**: 761处Docstring缺失
- **修复方案**: 补充核心类文- **责任*: Python开- **截止日期**: 2026-06-15

### TODO-019: 合并重复文档
- **状态*: 待处- **优先*: Low
- **问题**: docs/DEPLOYMENT.md 重复
- **修复方案**: 合并或明确区- **责任*: 技术文- **截止日期**: 2026-06-10

### TODO-020: CHANGELOG日期补充
- **状态*: 待处- **优先*: Low
- **问题**: CHANGELOG缺少日期
- **修复方案**: 添加YYYY-MM-DD格式
- **责任*: 技术文- **截止日期**: 2026-06-10

### TODO-021: 非root用户配置
- **状态*: 待处- **优先*: Low
- **问题**: Dockerfile未配置非root用户
- **修复方案**: 添加USER指令
- **责任*: DevOps
- **截止日期**: 2026-06-15

### TODO-022: 差异化资源限- **状态*: 待处- **优先*: Low
- **问题**: docker-compose.yml资源限制过于统一
- **修复方案**: 按服务需求差异化配置
- **责任*: DevOps
- **截止日期**: 2026-06-15

### TODO-023: OpenAPI文档聚合
- **状态*: 待处- **优先*: Low
- **问题**: 缺少OpenAPI聚合
- **修复方案**: 网关层添加springdoc
- **责任*: 后端开- **截止日期**: 2026-06-20

### TODO-024: 边缘服务技术栈明确
- **状态*: 待处- **优先*: Low
- **问题**: edge-cloud-coordinator文档与实现不- **修复方案**: 明确为Python实现或补充Java实现
- **责任*: 架构建
- **截止日期**: 2026-06-20

### TODO-025: 批量处理熔断阈值优- **状态*: 待处- **优先*: Low
- **问题**: 失败率阈0%过高
- **修复方案**: 降低0%
- **责任*: 后端开- **截止日期**: 2026-06-20

---

## 已完### DONE-001: 修复wrf-processor-service密码
- **完成日期**: 2026-05-09
- **修改**: 移除 `${DB_PASSWORD:123456}` 的默认### DONE-002: 修复path-planning-service密码
- **完成日期**: 2026-05-09
- **修改**: 移除 `${DB_PASSWORD:123456}` 的默认### DONE-003: 修复meteor-forecast-service密码
- **完成日期**: 2026-05-09
- **修改**: 移除 `${DB_PASSWORD:123456}` 的默认### DONE-004: 修复uav-weather-collector密码
- **完成日期**: 2026-05-09
- **修改**: 移除 `${DB_PASSWORD:uav123}` 的默认### DONE-005: uav-platform-service密码已正确配置
- **状态*: 确认
- **说明**: 该文件已正确使用 `${DB_PASSWORD}` 无默认### DONE-006: Feign Client集成 - PlatformController
- **完成日期**: 2026-05-09
- **文件**: uav-platform-service/src/main/java/com/uav/platform/controller/PlatformController.java
- **修改**: 重构为使用Feign Client替代RestTemplate直接调用

### DONE-007: Feign Client集成 - WeatherCollectorCircuitBreakerService
- **完成日期**: 2026-05-09
- **文件**: uav-weather-collector/src/main/java/com/uav/weather/resilience/WeatherCollectorCircuitBreakerService.java
- **修改**: 重构为使用Feign Client + Resilience4j熔断### DONE-008: 统一Python脚本调用- **完成日期**: 2026-05-09
- **文件**: common-utils/src/main/java/com/uav/common/feign/PythonScriptInvoker.java
- **说明**: 创建统一的Python脚本执行器包含安全验证超时控制重试机### DONE-009: Feign Clients配置
- **完成日期**: 2026-05-09
- **文件**: common-utils/src/main/java/com/uav/common/feign/FeignClientsConfig.java
- **文件**: common-utils/pom.xml (添加Feign依赖)
- **说明**: 创建Feign统一配置### DONE-010: 迁移指南文档
- **完成日期**: 2026-05-09
- **文件**: docs/FEIGN_MIGRATION_GUIDE.md
- **说明**: 创建详细的Feign迁移指南文档

---

## 统计

| 状态| 数量 |
|------|------|
| 待处(P0) | 2 |
| 待处(P1) | 4 |
| 待处(P2) | 8 |
| 待处(P3) | 9 |
| **已完* | **15** |
| **总计** | **38** |

---

*最后更新 2026-05-09*
*维护者 AI代码审计系统*

