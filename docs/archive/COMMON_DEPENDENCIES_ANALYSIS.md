# 公共依赖分析报告

> **注意**: 此文件为历史归档报告。

## 分析结果

### 公共依赖提取
已将以下公共依赖统一提取至根 POM `<dependencyManagement>`：
- Spring Boot 3.2.0
- Spring Cloud 2023.0.3
- Spring Cloud Alibaba 2023.0.1
- Lombok
- SLF4J
- Resilience4j
- Springdoc OpenAPI 2.3.0

### 模块依赖优化
- 各微服务模块精简依赖声明
- 版本号统一管理
- 避免依赖冲突

---

> **归档日期**: 2026-05-09  
> **维护者**: DITHIOTHREITOL