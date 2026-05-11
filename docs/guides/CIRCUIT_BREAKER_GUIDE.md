# UAV Platform Circuit Breaker Implementation Guide

## 熔断器(Circuit Breaker)实现指南

---

## 为什么需要熔断器？

### 问题背景

在微服务架构中，服务间调用可能会出现以下问题：

1. **级联故障**: 一个服务不可用导致调用它的服务也超时，最终整个系统瘫痪
2. **资源耗尽**: 大量请求堆积在等待中，耗尽线程池和连接池
3. **用户体验差**: 用户请求长时间等待最终得到超时错误

### 熔断器模式

熔断器模式类似于电路保险丝：
- **闭合状态(Closed)**: 正常请求通过，失败率升高时切换
- **打开状态(Open)**: 快速失败，直接返回错误，不调用下游服务
- **半开状态(Half-Open)**: 尝试放行部分请求测试服务是否恢复

---

## 已实现的熔断器

### 服务列表

| 服务名称 | 熔断器配置 | 失败率阈值 | 恢复等待时间 |
|---------|-----------|:---------:|:----------:|
| wrf-processor-service | WrfProcessorCB | 50% | 30s |
| meteor-forecast-service | ForecastCB | 50% | 30s |
| path-planning-service | PathPlanningCB | 50% | 30s |
| data-assimilation-service | AssimilationCB | 50% | 30s |
| uav-weather-collector | WeatherCollectorCB | 50% | 30s |

### 配置示例

```yaml
resilience4j:
  circuitbreaker:
    instances:
      wrf-processor:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
        minimum-number-of-calls: 5
```

### 使用方式

```java
@Service
public class WrfProcessorService {
    @CircuitBreaker(name = "wrf-processor", fallbackMethod = "fallbackProcess")
    public String processWrfData(String filePath) {
        // 业务逻辑
    }
    
    public String fallbackProcess(String filePath, Exception e) {
        // 降级处理
        return "WRF processing temporarily unavailable";
    }
}
```

---

> **最后更新**: 2026-05-09  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL