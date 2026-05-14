# tests

UAV 路径规划系统测试套件，覆盖单元测试、集成测试、端到端测试、性能测试、安全测试和混沌工程测试六大类型，确保系统质量与可靠性。

## 目录结构

```
tests/
├── e2e/                          # 端到端测试
│   ├── test_e2e_flows.py         # E2E 业务流程测试
│   └── pytest.ini                # E2E 专用 pytest 配置
├── performance/                  # 性能测试
│   └── uav-path-planning-jmeter.jmx  # JMeter 性能测试计划
├── test_basic.py                 # 基础结构/完整性测试
├── test_system.py                # 服务间调用系统测试
├── test_integration.py           # 数据同化集成测试
├── test_performance.py           # 性能基准测试
├── test_security.py              # 安全模块测试 (mTLS/JWT/加密)
├── test_coordinator.py           # 边缘云协调器测试
├── test_algorithm.py             # 算法核心测试
├── test_optimized_algorithm.py   # 优化算法测试
├── test_ai_decision.py           # AI 决策测试
├── test_federated_learning.py    # 联邦学习测试
├── test_exception_specificity.py # 异常精细化测试
├── chaos_test_suite.py           # 混沌工程测试套件
├── check_syntax.py               # Python 语法检查
├── check_security.py             # 安全漏洞扫描
├── check_imports.py              # 无用导入检查
├── check_system.py               # 系统完整性检查
├── fix_*.py                      # 自动修复脚本
├── requirements.txt              # 测试依赖
└── TESTING_GUIDE.md              # 测试规范指南
```

## 测试类型

| 测试类型 | 目录/文件 | 框架 | 说明 |
|---------|----------|------|------|
| 单元测试 | `test_basic.py`, `test_security.py`, `test_coordinator.py` 等 | unittest / pytest | 验证独立模块与函数 |
| 集成测试 | `test_integration.py`, `test_system.py` | pytest | 跨服务/Mock数据联动测试 |
| 端到端测试 | `e2e/test_e2e_flows.py` | pytest + requests | 完整业务流程验证 |
| 性能测试 | `test_performance.py`, `performance/` | pytest + JMeter | 响应时间/吞吐量/资源 |
| 安全测试 | `test_security.py`, `check_security.py` | unittest + 自定义 | JWT/mTLS/数据加密/漏洞扫描 |
| 混沌工程 | `chaos_test_suite.py` | requests + threading | 弹性验证/故障恢复 |

## 快速开始

### 安装测试依赖

```bash
pip install -r tests/requirements.txt
```

### 运行测试

```bash
# 运行所有 Python 测试
pytest tests/ -v

# 运行单元测试
pytest tests/test_basic.py tests/test_security.py tests/test_coordinator.py -v

# 运行集成测试
pytest tests/test_integration.py tests/test_system.py -v

# 运行 E2E 测试
pytest tests/e2e/ -v --headed

# 运行性能测试
pytest tests/test_performance.py -v -m performance

# 运行混沌测试
python tests/chaos_test_suite.py

# 安全扫描
python tests/check_security.py

# 语法检查
python tests/check_syntax.py

# 生成覆盖率报告
pytest tests/ --cov --cov-report=html
```

### 环境变量

E2E 和系统测试需要设置以下环境变量：

```bash
export BASE_URL=http://localhost:8088
export TEST_USERNAME=your_test_user
export TEST_PASSWORD=your_test_password
```

## Java 测试

Java 模块单元测试通过 Maven Surefire 执行：

```bash
# 运行所有 Java 测试
mvn test

# 运行特定模块测试
mvn test -pl common-utils -am

# 生成覆盖率报告
mvn jacoco:report

# 检查覆盖率阈值 (80% line, 70% branch)
mvn jacoco:check
```

## 测试覆盖率要求

| 模块 | Line | Branch |
|------|:----:|:------:|
| common-utils | 90%+ | 80%+ |
| api-gateway | 85%+ | 75%+ |
| wrf-processor-service | 85%+ | 75%+ |
| meteor-forecast-service | 85%+ | 75%+ |
| path-planning-service | 85%+ | 75%+ |
| data-assimilation-service | 85%+ | 75%+ |
| uav-platform-service | 80%+ | 70%+ |
| uav-path-planning-system | 80%+ | 70%+ |
| uav-weather-collector | 80%+ | 70%+ |
| data-assimilation-platform | 80%+ | 70%+ |

## CI/CD 集成

测试在 GitHub Actions 中自动运行，每次 push 触发：

1. `mvn test` — Java 单元测试
2. `pytest tests/ -v` — Python 测试
3. `python tests/check_security.py` — 安全扫描
4. `mvn jacoco:check` — 覆盖率检查
5. `mvn dependency-check:check` — 依赖漏洞扫描

## 新增测试规范

1. 测试类使用 `@DisplayName` 注解描述测试目的
2. 遵循 AAA 模式 (Arrange-Act-Assert)
3. 每个测试独立可重复
4. Mock 外部依赖使用 Mockito (Java) / unittest.mock (Python)
5. Python 测试文件以 `test_` 前缀命名

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
