# 熔断器使用示例

## 基本用法

### 1. 在Service类中使用熔断器

```java
@Service
public class WrfProcessorService {
    
    @CircuitBreaker(name = "wrf-processor", fallbackMethod = "fallbackProcess")
    public String processWrfData(String filePath) {
        // 调用WRF处理逻辑
        return wrfClient.process(filePath);
    }
    
    public String fallbackProcess(String filePath, Exception e) {
        log.warn("WRF processing fallback triggered: {}", e.getMessage());
        return "WRF processing temporarily unavailable. Please try again later.";
    }
}
```

### 2. 配置熔断器参数

```yaml
resilience4j:
  circuitbreaker:
    instances:
      wrf-processor:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
        minimum-number-of-calls: 5
        permitted-number-of-calls-in-half-open-state: 3
```

### 3. 组合使用Retry + CircuitBreaker

```java
@Retry(name = "wrf-retry", fallbackMethod = "fallbackRetry")
@CircuitBreaker(name = "wrf-processor", fallbackMethod = "fallbackCircuit")
public String processWrfData(String filePath) {
    return wrfClient.process(filePath);
}
```

---

## 断路器状态监控

### 通过Actuator查看状态

```
GET /actuator/circuitbreakers
GET /actuator/circuitbreakerevents
```

---

> **最后更新**: 2026-05-09  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL