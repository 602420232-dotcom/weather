// service_spring/src/main/java/com/bayesian/client/PythonServiceClient.java

package com.bayesian.client;

import com.bayesian.dto.request.AssimilationRequest;
import com.bayesian.dto.response.AssimilationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
public class PythonServiceClient {
    
    private final WebClient webClient;
    
    @Autowired
    public PythonServiceClient(WebClient.Builder webClientBuilder, 
                             @Value("${python.service.url}") String pythonServiceUrl) {
        this.webClient = webClientBuilder
            .baseUrl(pythonServiceUrl)
            .build();
    }
    
    public Mono<AssimilationResponse> computeAssimilation(AssimilationRequest request) {
        log.info("调用Python服务执行同化计算，JobID: {}", request.getJobId());
        
        return webClient.post()
            .uri("/api/v1/assimilation/compute")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(AssimilationResponse.class)
            .timeout(Duration.ofSeconds(30))
            .doOnError(error -> log.error("Python服务调用失败: {}", error.getMessage()))
            .doOnSuccess(response -> log.info("Python服务调用成功，JobID: {}", response.getJobId()));
    }
    
    public Mono<Boolean> healthCheck() {
        return webClient.get()
            .uri("/health")
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> response.contains("healthy"))
            .timeout(Duration.ofSeconds(5))
            .onErrorReturn(false);
    }
}