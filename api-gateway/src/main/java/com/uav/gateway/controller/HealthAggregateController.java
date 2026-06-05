package com.uav.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * 聚合健康检查控制器
 * 汇总所有微服务的健康状态到单一端点
 */
@RestController
public class HealthAggregateController {

    private static final Set<String> EXCLUDED_SERVICES = Set.of("api-gateway");
    private static final int TIMEOUT_MS = 5000;

    @Autowired
    private DiscoveryClient discoveryClient;

    private final WebClient webClient;

    public HealthAggregateController() {
        this.webClient = WebClient.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(16 * 1024))
                .build();
    }

    @GetMapping("/actuator/health/aggregate")
    public Mono<Map<String, Object>> aggregateHealth() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gateway", "UP");
        result.put("timestamp", System.currentTimeMillis());

        List<String> services = discoveryClient.getServices().stream()
                .filter(s -> !EXCLUDED_SERVICES.contains(s))
                .sorted()
                .collect(Collectors.toList());

        if (services.isEmpty()) {
            result.put("services", new LinkedHashMap<>());
            result.put("status", "UP");
            result.put("serviceCount", 0);
            return Mono.just(result);
        }

        return Flux.fromIterable(services)
                .flatMap(this::checkServiceHealth)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue, LinkedHashMap::new)
                .map(servicesHealth -> {
                    boolean allUp = servicesHealth.values().stream().allMatch("UP"::equals);
                    result.put("services", servicesHealth);
                    result.put("status", allUp ? "UP" : "DEGRADED");
                    result.put("serviceCount", services.size());
                    return result;
                });
    }

    private Mono<Map.Entry<String, String>> checkServiceHealth(String serviceId) {
        String url = "http://" + serviceId + "/actuator/health";
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(body -> {
                    if (body != null && body.contains("\"status\":\"UP\"")) {
                        return Map.entry(serviceId, "UP");
                    }
                    return Map.entry(serviceId, "DEGRADED");
                })
                .onErrorReturn(Map.entry(serviceId, "DOWN"))
                .timeout(java.time.Duration.ofMillis(TIMEOUT_MS))
                .onErrorReturn(Map.entry(serviceId, "TIMEOUT"));
    }
}
