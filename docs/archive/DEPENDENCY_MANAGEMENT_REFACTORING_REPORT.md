# 依赖管理重构报告

> **注意**: 此文件为历史归档报告。

## 重构摘要

### 合并项
- common-dependencies 模块合并至父 POM
- 统一版本管理在根 POM 的 `<dependencyManagement>` 中
- Spring Cloud 版本升级至 2023.0.3

### 修复项
- spring-cloud-starter-bootstrap 显式版本
- jython-standalone GAV 修复
- 各模块缺失依赖补充 (validation, resilience4j)

---

> **归档日期**: 2026-05-09  
> **维护者**: DITHIOTHREITOL