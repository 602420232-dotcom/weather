package com.uav.bayesian.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class PythonServiceClient {

    private final RestTemplate restTemplate;

    public PythonServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String executeAssimilation(String pythonServiceUrl, Object request) {
        log.debug("调用 Python 同化服务: {}", pythonServiceUrl);
        try {
            return restTemplate.postForObject(pythonServiceUrl + "/assimilate", request, String.class);
        } catch (Exception e) {
            log.error("调用 Python 服务失败: {}", e.getMessage());
            return "{\"status\":\"error\",\"message\":\"Python service call failed\"}";
        }
    }
}
