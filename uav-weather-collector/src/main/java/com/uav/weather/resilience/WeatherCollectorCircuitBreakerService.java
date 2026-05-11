package com.uav.weather.resilience;

import com.uav.common.exception.ServiceUnavailableException;
import com.uav.common.feign.BuoyWeatherClient;
import com.uav.common.feign.GroundStationWeatherClient;
import com.uav.common.feign.SatelliteWeatherClient;
import com.uav.common.feign.WrfProcessorClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 气象采集熔断器服务
 * 
 * 提供对外部气象数据源的熔断保护，支持多种气象数据源：
 * - WRF气象模型
 * - 卫星气象数据
 * - 地面气象站
 * - 浮标气象站
 * 
 * 已重构为使用Feign Client进行服务调用，
 * 配合Resilience4j熔断器实现弹性架构。
 */
@Slf4j
@Service
public class WeatherCollectorCircuitBreakerService {

    private final WrfProcessorClient wrfProcessorClient;
    private final SatelliteWeatherClient satelliteWeatherClient;
    private final GroundStationWeatherClient groundStationWeatherClient;
    private final BuoyWeatherClient buoyWeatherClient;
    
    // 熔断器注册表
    private CircuitBreakerRegistry registry;
    
    // 各数据源熔断器
    private CircuitBreaker wrfCircuitBreaker;
    private CircuitBreaker satelliteCircuitBreaker;
    private CircuitBreaker groundStationCircuitBreaker;
    private CircuitBreaker buoyCircuitBreaker;
    
    // 配置
    @Value("${weather.circuit-breaker.failure-rate-threshold:60}")
    private double failureRateThreshold;
    
    @Value("${weather.circuit-breaker.wait-duration-in-open-state:30s}")
    private String waitDurationInOpenState;
    
    @Value("${weather.circuit-breaker.sliding-window-size:20}")
    private int slidingWindowSize;

    /**
     * 构造函数
     * 
     * @param wrfProcessorClient WRF处理器Feign客户端
     * @param satelliteWeatherClient 卫星气象服务Feign客户端
     * @param groundStationWeatherClient 地面气象站服务Feign客户端
     * @param buoyWeatherClient 浮标气象服务Feign客户端
     */
    public WeatherCollectorCircuitBreakerService(WrfProcessorClient wrfProcessorClient,
                                                SatelliteWeatherClient satelliteWeatherClient,
                                                GroundStationWeatherClient groundStationWeatherClient,
                                                BuoyWeatherClient buoyWeatherClient) {
        this.wrfProcessorClient = wrfProcessorClient;
        this.satelliteWeatherClient = satelliteWeatherClient;
        this.groundStationWeatherClient = groundStationWeatherClient;
        this.buoyWeatherClient = buoyWeatherClient;
    }

    @PostConstruct
    public void init() {
        // 创建熔断器配置
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold((float) failureRateThreshold)
                .slowCallRateThreshold(80)
                .waitDurationInOpenState(Duration.parse("PT" + waitDurationInOpenState.replace("s", "") + "S"))
                .slidingWindowSize(slidingWindowSize)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        
        // 创建熔断器注册表
        registry = CircuitBreakerRegistry.of(config);
        
        // 为各数据源创建熔断器
        wrfCircuitBreaker = registry.circuitBreaker("wrf-weather");
        satelliteCircuitBreaker = registry.circuitBreaker("satellite-weather");
        groundStationCircuitBreaker = registry.circuitBreaker("ground-station-weather");
        buoyCircuitBreaker = registry.circuitBreaker("buoy-weather");
        
        // 注册事件监听器
        registerEventListeners();
        
        log.info("Weather Collector Circuit Breaker initialized: failureRateThreshold={}, waitDuration={}", 
                failureRateThreshold, waitDurationInOpenState);
    }

    private void registerEventListeners() {
        // WRF熔断器监听
        wrfCircuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.warn("WRF Circuit Breaker state transition: {} -> {}", 
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()));
        
        // 卫星熔断器监听
        satelliteCircuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.warn("Satellite Circuit Breaker state transition: {} -> {}",
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()));
        
        // 地面站熔断器监听
        groundStationCircuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.warn("Ground Station Circuit Breaker state transition: {} -> {}",
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()));
        
        // 浮标熔断器监听
        buoyCircuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.warn("Buoy Circuit Breaker state transition: {} -> {}",
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()));
    }

    /**
     * 调用WRF气象模型（带熔断保护）
     * 
     * 使用Feign Client配合Resilience4j熔断器，
     * 当失败率超过阈值时自动熔断，防止雪崩效应。
     * 
     * @param request WRF数据解析请求
     * @return 解析结果
     */
    public Map<String, Object> callWRFModel(Map<String, Object> request) {
        try {
            return wrfCircuitBreaker.executeSupplier(() -> {
                log.debug("Calling WRF model via Feign Client");
                Map<String, Object> response = wrfProcessorClient.parseWrfData(request);
                
                // 检查响应是否成功
                if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                    throw new RuntimeException("WRF model returned error: " + response);
                }
                
                return response;
            });
        } catch (Exception e) {
            log.error("WRF model call failed, circuit breaker may be open", e);
            throw ServiceUnavailableException.serviceDown("wrf-weather", "WRF气象模型暂时不可用");
        }
    }

    /**
     * 获取WRF数据列表（带熔断保护）
     * 
     * @param page 页码
     * @param size 每页数量
     * @return WRF数据列表
     */
    public Map<String, Object> getWrfDataList(int page, int size) {
        try {
            return wrfCircuitBreaker.executeSupplier(() -> {
                log.debug("Getting WRF data list: page={}, size={}", page, size);
                return wrfProcessorClient.listWrfData(page, size);
            });
        } catch (Exception e) {
            log.error("Failed to get WRF data list", e);
            throw ServiceUnavailableException.serviceDown("wrf-weather", "WRF数据查询暂时不可用");
        }
    }

    /**
     * 调用卫星气象数据（带熔断保护）
     * 
     * 使用Feign Client配合Resilience4j熔断器，
     * 当失败率超过阈值时自动熔断，防止雪崩效应。
     * 
     * @param request 卫星数据请求（包含region和channel参数）
     * @return 卫星数据
     */
    public Map<String, Object> callSatellite(Map<String, Object> request) {
        try {
            return satelliteCircuitBreaker.executeSupplier(() -> {
                log.debug("Calling satellite weather data source: {}", request);
                
                // 从请求中获取参数
                String region = request != null && request.get("region") != null 
                        ? String.valueOf(request.get("region")) : "CHINA";
                String channel = request != null && request.get("channel") != null 
                        ? String.valueOf(request.get("channel")) : "IR";
                
                Map<String, Object> response = satelliteWeatherClient.getCloudImage(region, channel);
                
                // 检查响应是否成功
                if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                    throw new RuntimeException("Satellite weather service returned error: " + response);
                }
                
                return response;
            });
        } catch (Exception e) {
            log.error("Satellite call failed, circuit breaker may be open", e);
            throw ServiceUnavailableException.serviceDown("satellite-weather", "卫星气象数据暂时不可用");
        }
    }

    /**
     * 调用地面气象站（带熔断保护）
     * 
     * 使用Feign Client配合Resilience4j熔断器，
     * 当失败率超过阈值时自动熔断，防止雪崩效应。
     * 
     * @param request 地面站请求（包含stationId参数）
     * @return 地面站数据
     */
    public Map<String, Object> callGroundStation(Map<String, Object> request) {
        try {
            return groundStationCircuitBreaker.executeSupplier(() -> {
                log.debug("Calling ground station weather data source: {}", request);
                
                // 从请求中获取站点ID
                String stationId = request != null && request.get("stationId") != null 
                        ? String.valueOf(request.get("stationId")) : null;
                
                Map<String, Object> response = groundStationWeatherClient.getStationData(stationId);
                
                // 检查响应是否成功
                if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                    throw new RuntimeException("Ground station weather service returned error: " + response);
                }
                
                return response;
            });
        } catch (Exception e) {
            log.error("Ground station call failed, circuit breaker may be open", e);
            throw ServiceUnavailableException.serviceDown("ground-station-weather", "地面气象站暂时不可用");
        }
    }

    /**
     * 调用浮标气象站（带熔断保护）
     * 
     * 使用Feign Client配合Resilience4j熔断器，
     * 当失败率超过阈值时自动熔断，防止雪崩效应。
     * 
     * @param request 浮标请求（包含buoyId参数）
     * @return 浮标数据
     */
    public Map<String, Object> callBuoy(Map<String, Object> request) {
        try {
            return buoyCircuitBreaker.executeSupplier(() -> {
                log.debug("Calling buoy weather data source: {}", request);
                
                // 从请求中获取浮标ID
                String buoyId = request != null && request.get("buoyId") != null 
                        ? String.valueOf(request.get("buoyId")) : null;
                
                Map<String, Object> response = buoyWeatherClient.getBuoyData(buoyId);
                
                // 检查响应是否成功
                if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                    throw new RuntimeException("Buoy weather service returned error: " + response);
                }
                
                return response;
            });
        } catch (Exception e) {
            log.error("Buoy call failed, circuit breaker may be open", e);
            throw ServiceUnavailableException.serviceDown("buoy-weather", "浮标气象站暂时不可用");
        }
    }

    /**
     * 获取所有熔断器状态
     * 
     * @return 各熔断器的详细状态信息
     */
    public Map<String, Object> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("wrf-weather", getBreakerStatus(wrfCircuitBreaker));
        status.put("satellite-weather", getBreakerStatus(satelliteCircuitBreaker));
        status.put("ground-station-weather", getBreakerStatus(groundStationCircuitBreaker));
        status.put("buoy-weather", getBreakerStatus(buoyCircuitBreaker));
        
        return status;
    }

    private Map<String, Object> getBreakerStatus(CircuitBreaker breaker) {
        Map<String, Object> status = new HashMap<>();
        status.put("name", breaker.getName());
        status.put("state", breaker.getState().toString());
        status.put("failureRate", breaker.getMetrics().getFailureRate());
        status.put("slowCallRate", breaker.getMetrics().getSlowCallRate());
        status.put("successfulCalls", breaker.getMetrics().getNumberOfSuccessfulCalls());
        status.put("failedCalls", breaker.getMetrics().getNumberOfFailedCalls());
        status.put("notPermittedCalls", breaker.getMetrics().getNumberOfNotPermittedCalls());
        status.put("bufferedCalls", breaker.getMetrics().getNumberOfBufferedCalls());
        return status;
    }

    /**
     * 手动触发熔断
     * 
     * @param name 熔断器名称
     */
    public void tripCircuitBreaker(String name) {
        CircuitBreaker breaker = registry.circuitBreaker(name);
        breaker.transitionToOpenState();
        log.warn("Circuit breaker {} manually tripped", name);
    }

    /**
     * 手动重置熔断器
     * 
     * @param name 熔断器名称
     */
    public void resetCircuitBreaker(String name) {
        CircuitBreaker breaker = registry.circuitBreaker(name);
        breaker.transitionToClosedState();
        log.info("Circuit breaker {} manually reset", name);
    }

    /**
     * 获取熔断器配置信息
     * 
     * @return 当前熔断器配置
     */
    public Map<String, Object> getCircuitBreakerConfig() {
        return Map.of(
                "failureRateThreshold", failureRateThreshold,
                "waitDurationInOpenState", waitDurationInOpenState,
                "slidingWindowSize", slidingWindowSize
        );
    }
}
