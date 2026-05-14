# e2e

端到端 (E2E) 测试套件，基于 pytest + requests 验证 UAV 路径规划系统关键业务流程的完整性。

## 文件说明

| 文件 | 说明 |
|------|------|
| `test_e2e_flows.py` | E2E 业务流程测试主文件 |
| `pytest.ini` | E2E 专用 pytest 配置 |

## 覆盖的关键业务流程

### 1. 用户认证流程 (`TestAuthenticationFlow`)

| 测试用例 | 说明 |
|---------|------|
| `test_01_register_user` | 新用户注册 |
| `test_02_login_success` | 登录成功获取 JWT Token |
| `test_03_login_failure_wrong_password` | 错误密码登录失败 |
| `test_04_unauthorized_access_denied` | 未认证请求被拒绝 |

### 2. 数据源管理流程 (`TestDataSourceManagement`)

| 测试用例 | 说明 |
|---------|------|
| `test_01_list_datasources` | 获取数据源列表 |
| `test_02_create_datasource` | 创建新数据源 |
| `test_03_get_datasource_types` | 获取数据源类型列表 |
| `test_04_delete_datasource` | 删除数据源 |

### 3. 气象数据获取流程 (`TestWeatherDataFlow`)

| 测试用例 | 说明 |
|---------|------|
| `test_01_get_weather_data` | 获取 WRF 处理后的气象数据 |
| `test_02_get_real_ground_station_data` | 获取实时地面站数据 |
| `test_03_get_buoy_data` | 获取浮标数据 |

### 4. 路径规划流程 (`TestPathPlanningFlow`)

| 测试用例 | 说明 |
|---------|------|
| `test_01_plan_path_request` | 提交路径规划请求 |
| `test_02_get_planning_history` | 获取规划历史记录 |

### 5. API 网关健康检查 (`TestGatewayHealth`)

| 测试用例 | 说明 |
|---------|------|
| `test_01_gateway_health` | 网关存活检查 |
| `test_02_wrf_processor_health` | WRF 处理服务健康 |
| `test_03_nacos_discovery` | Nacos 服务发现可达性 |

### 6. 弹性测试 (`TestResilienceEndToEnd`)

| 测试用例 | 说明 |
|---------|------|
| `test_01_retry_on_timeout` | 验证重试机制 |
| `test_02_bulk_requests` | 批量请求不崩溃 (5次连续请求) |

## 快速开始

### 环境准备

```bash
pip install pytest requests
```

### 设置环境变量

```bash
# Windows PowerShell
$env:BASE_URL="http://localhost:8088"
$env:TEST_USERNAME="your_test_user"
$env:TEST_PASSWORD="your_test_password"

# Linux / macOS
export BASE_URL=http://localhost:8088
export TEST_USERNAME=your_test_user
export TEST_PASSWORD=your_test_password
```

### 运行测试

```bash
# 运行所有 E2E 测试 (顺序执行)
pytest tests/e2e/test_e2e_flows.py -v -s

# 运行特定测试类
pytest tests/e2e/test_e2e_flows.py::TestAuthenticationFlow -v
pytest tests/e2e/test_e2e_flows.py::TestPathPlanningFlow -v

# 运行特定测试用例
pytest tests/e2e/test_e2e_flows.py::TestAuthenticationFlow::test_02_login_success -v

# 仅运行健康检查
pytest tests/e2e/test_e2e_flows.py::TestGatewayHealth -v

# 运行弹性测试
pytest tests/e2e/test_e2e_flows.py::TestResilienceEndToEnd -v
```

## 注意事项

- E2E 测试需要完整的服务栈运行中，建议在 CI/CD 的集成测试阶段执行
- 部分测试用例对非关键失败做了容错处理（如 `assert resp.status_code in [200, 404, 503]`），避免因服务未部署而阻塞
- 测试假设 API 网关地址为 `http://localhost:8088`，可通过 `BASE_URL` 环境变量覆盖

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
