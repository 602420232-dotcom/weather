#  UAV Path Planning System - Documentation Index

##  我需..

###  快速开| 任务 | 文档 |
:|------|------|
| 了解项目 | [README.md](../README.md) |
| 快速部| [DEPLOYMENT.md](DEPLOYMENT.md) |
| 常用命令速查 | [QUICK_REFERENCE.md](QUICK_REFERENCE.md) |

### 系统架构
| 任务 | 文档 |
:|------|------|
| 了解项目结构 | [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) |
| 架构设计详解 | [architecture.md](architecture.md) |
| 服务依赖关系 | [PROJECT_STRUCTURE.md#服务依赖关系图](PROJECT_STRUCTURE.md#服务依赖关系|

### 安全配置
| 任务 | 文档 |
:|------|------|
| JWT配置 | [PRODUCTION_SECRETS_GUIDE.md](guides/PRODUCTION_SECRETS_GUIDE.md) |
| 安全改进报告 | [SECURITY_IMPROVEMENTS.md](archive/SECURITY_IMPROVEMENTS.md) |

### 熔断器(Circuit Breaker)
| 任务 | 文档 |
:|------|------|
| 熔断器使用指| [CIRCUIT_BREAKER_GUIDE.md](guides/CIRCUIT_BREAKER_GUIDE.md) |
| 代码示例 | [CIRCUIT_BREAKER_USAGE_EXAMPLES.md](guides/CIRCUIT_BREAKER_USAGE_EXAMPLES.md) |
| 实现报告 | [CIRCUIT_BREAKER_IMPLEMENTATION_COMPLETE_REPORT.md](archive/CIRCUIT_BREAKER_IMPLEMENTATION_COMPLETE_REPORT.md) |

###  监控与日| 任务 | 文档 |
:|------|------|
| 监控配置 | [deployments/monitoring/README.md](../deployments/monitoring/README.md) |
| 故障排除指南 | [TROUBLESHOOTING.md](guides/TROUBLESHOOTING.md) |

###  部署运维
| 任务 | 文档 |
:|------|------|
| Docker部署 | [DOCKER.md](DOCKER.md) |
| Kubernetes部署 | [deployment/DEPLOYMENT.md](deployment/DEPLOYMENT.md) |
| 生产环境配置 | [PRODUCTION_SECRETS_GUIDE.md](guides/PRODUCTION_SECRETS_GUIDE.md) |

###  代码质量
| 任务 | 文档 |
:|------|------|
| 质量评估报告 | [COMPREHENSIVE_QUALITY_ASSESSMENT.md](reports/COMPREHENSIVE_QUALITY_ASSESSMENT.md) |
| 综合审计报告 | [COMPREHENSIVE_AUDIT_REPORT_v2.1.md](reports/COMPREHENSIVE_AUDIT_REPORT_v2.1.md) |

###  API文档
| 任务 | 文档 |
:|------|------|
| API总览 | [api/README.md](api/README.md) |
| API文档 | [API_DOCUMENTATION.md](api/API_DOCUMENTATION.md) |

###  测试
| 任务 | 文档 |
:|------|------|
| 测试指南 | [TESTING_GUIDE.md](../tests/TESTING_GUIDE.md) |
| 性能测试 | [test_performance.py](../tests/test_performance.py) |

---

##  文档分类

###  新手入门
1. [README.md](../README.md) - 项目概述
2. [DEPLOYMENT.md](DEPLOYMENT.md) - 部署指南
3. [QUICK_REFERENCE.md](QUICK_REFERENCE.md) - 快速参### 开发人员
1. [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) - 项目结构
2. [CIRCUIT_BREAKER_GUIDE.md](guides/CIRCUIT_BREAKER_GUIDE.md) - 熔断器使3. [api/README.md](api/README.md) - API文档

### ?安全运维
1. [PRODUCTION_SECRETS_GUIDE.md](guides/PRODUCTION_SECRETS_GUIDE.md) - 安全配置
2. [SECURITY_IMPROVEMENTS.md](archive/SECURITY_IMPROVEMENTS.md) - 安全改进
3. [deployments/monitoring/README.md](../deployments/monitoring/README.md) - 监控运维

###  项目管理
1. [COMPREHENSIVE_QUALITY_ASSESSMENT.md](reports/COMPREHENSIVE_QUALITY_ASSESSMENT.md) - 质量评估
2. [CHANGELOG.md](CHANGELOG.md) - 更新日志
3. [COMPREHENSIVE_AUDIT_REPORT_v2.1.md](reports/COMPREHENSIVE_AUDIT_REPORT_v2.1.md) - 综合审计

---

##  按文件名查找

| 文件| 说明 |
|--------|------|
| [README.md](../README.md) | 项目总览 |
| [DEPLOYMENT.md](DEPLOYMENT.md) | 部署指南 |
| [DOCKER.md](DOCKER.md) | Docker说明 |
| [CHANGELOG.md](CHANGELOG.md) | 更新日志 |
| [QUICK_REFERENCE.md](QUICK_REFERENCE.md) | 快速参|
| [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) | 项目结构 |
| [CIRCUIT_BREAKER_GUIDE.md](guides/CIRCUIT_BREAKER_GUIDE.md) | 熔断器指|
| [PRODUCTION_SECRETS_GUIDE.md](guides/PRODUCTION_SECRETS_GUIDE.md) | 生产配置 |
| [COMPREHENSIVE_QUALITY_ASSESSMENT.md](reports/COMPREHENSIVE_QUALITY_ASSESSMENT.md) | 质量评估 |
| [COMPREHENSIVE_AUDIT_REPORT_v2.1.md](reports/COMPREHENSIVE_AUDIT_REPORT_v2.1.md) | 综合审计 |
| [SECURITY_IMPROVEMENTS.md](archive/SECURITY_IMPROVEMENTS.md) | 安全改进 |

---

##  最近更### 2026-05-09
- ?熔断器实现完- ?common-utils模块文档完善
- ?所有Java服务添加熔断器保- ?监控配置完成
- ?文档引用错误修复

详见: [CHANGELOG.md](CHANGELOG.md)

---

##  提示

-  **收藏此页**: 快速访问所有文-  **使用搜索**: 按文件名或内容搜-  **查看源码**: 直接阅读 `src/` 下的代码注释
-  **报告问题**: ?GitHub Issues 提交
---

> **最后更新*: 2026-05-09
> **版本**: 2.1
> **维护者*: DITHIOTHREITOL

