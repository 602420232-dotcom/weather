# 贝叶斯同化服?

## 概述

贝叶斯同化服务整合了贝叶斯数据同化核心算法库通过 REST API 提供数据同化计算服务支?3D-VAR?D-VAREnKF 等算法用于融合多源气象观测数据?

## 技术栈

- **框架**: Spring Boot 3.2.0
- **语言**: Java 17 + Python 3.8+
- **构建工具**: Maven
- **算法引擎**: Python (bayesian_assimilation.py)
- **弹性机?*: Resilience4j Circuit Breaker

## 服务信息

- **服务端口**: 8084
- **服务名称**: data-assimilation-service
- **熔断?*: ✅ 已启用通过 common-utils?

## 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/assimilation/execute` | POST | 执行贝叶斯同?|
| `/api/assimilation/variance` | POST | 计算方差?|
| `/api/assimilation/batch` | POST | 批量执行同化 |
| `/api/admin/circuit-breaker/status` | GET | 熔断器状?|

## 熔断器配?

作为被调用服务data-assimilation-service 提供?API Gateway 的熔断器保护?

| 配置?| ✅ | 说明 |
|--------|-----|------|
| **失败率阈?* | 45% | 超过此阈值触发熔?|
| **恢复等待时间** | 8?| 熔断打开后等待时?|
| **滑动窗口** | 8次调?| 计算失败率的窗口大小 |
| **熔断器类?* | critical | 关键服务配置 |

详见: [Circuit Breaker Guide](../../docs/CIRCUIT_BREAKER_GUIDE.md)

## Python 依赖

| ✅ | 版本 | 用?|
|---|------|------|
| numpy | >=1.24.0 | 数值计?|
| scipy | >=1.11.0 | 科学计算 |

## 环境变量

| 变量 | 默认?| 说明 |
|------|--------|------|
| `DB_PASSWORD` | 必填 | 数据库密?|
| `assimilation.python-script` | `src/main/python/bayesian_assimilation.py` | Python 脚本路径 |
| `SERVER_PORT` | `8084` | 服务端口 |
| `JWT_SECRET` | 必填 | JWT签名密钥生产环境必需?|

## 构建与运?

```bash
# 构建
mvn clean package -DskipTests

# 运行
mvn spring-boot:run
```

## 配置

详见 `src/main/resources/application.yml`?

## 依赖服务

- **MySQL**: 数据存储启动前需运行?
- **Redis**: 缓存可选
- **uav-platform-service**: 认证服务可选
- **API Gateway**: 通过熔断器保护调?


---

> **最后更新*: 2026-05-09  
> **版本**: 2.1  
> **维护者*: DITHIOTHREITOL

