# performance

性能测试套件，使用 Apache JMeter 对 UAV 路径规划系统进行压力测试与性能基准验证。

## 文件说明

| 文件 | 说明 |
|------|------|
| `uav-path-planning-jmeter.jmx` | JMeter 性能测试计划 |

## 测试覆盖

JMeter 测试计划 (`uav-path-planning-jmeter.jmx`) 覆盖以下场景：

| 场景 | 说明 |
|------|------|
| 路径规划请求压测 | 模拟多用户并发提交路径规划任务 |
| 数据同化吞吐量 | 验证数据同化服务在高并发下的处理能力 |
| 气象数据接口基准 | 测试 WRF / 气象采集接口响应时间 |
| 认证接口并发 | JWT 认证/Token 刷新接口高并发验证 |
| API 网关路由性能 | 网关作为入口的性能基准 |

## 性能指标阈值

| 指标 | 目标值 | 告警阈值 |
|------|--------|---------|
| API 响应时间 (P95) | < 200ms | > 2s |
| API 响应时间 (P99) | < 500ms | > 5s |
| 错误率 | < 0.1% | > 5% |
| 吞吐量 (QPS) | > 100 | < 50 |
| CPU 使用率 | < 70% | > 80% |
| 内存使用率 | < 80% | > 85% |

## 快速开始

### 安装 JMeter

```bash
# 下载 JMeter 5.6+
# https://jmeter.apache.org/download_jmeter.cgi

# 或通过 chocolatacy (Windows)
choco install jmeter
```

### 运行测试

```bash
# GUI 模式 (调试/编辑)
jmeter -t tests/performance/uav-path-planning-jmeter.jmx

# 非 GUI 模式 (压测)
jmeter -n -t tests/performance/uav-path-planning-jmeter.jmx \
  -l results/performance.jtl \
  -e -o results/performance-report/

# 指定并发数
jmeter -n -t tests/performance/uav-path-planning-jmeter.jmx \
  -Jthreads=50 -Jrampup=30 -Jduration=300 \
  -l results/performance.jtl
```

### 自定义参数

```bash
-Jthreads=100     # 并发线程数 (默认: 20)
-Jrampup=60       # 启动时间(秒) (默认: 30)
-Jduration=600    # 测试持续时间(秒) (默认: 300)
-JbaseUrl=http://localhost:8088  # 目标服务地址
```

## Python 性能基准

除 JMeter 外，也可使用 pytest 运行轻量级性能基准：

```bash
# 运行 Python 性能基准测试
pytest tests/test_performance.py -v -m performance
```

`test_performance.py` 包含：
- 响应时间测试 (目标 < 100ms)
- 内存使用测试 (目标 < 512MB)
- 吞吐量测试 (目标 > 100 RPS)
- CPU 使用率测试 (目标 < 80%)

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
