package com.uav.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan(basePackages = "com.uav", excludeFilters = {
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.uav\\.common\\.exception\\..*"),
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.uav\\.common\\.config\\.CommonSecurityConfig")
})
@EnableDiscoveryClient
public class UavPlatformApplication {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    public static void main(String[] args) {
        SpringApplication.run(UavPlatformApplication.class, args);
    }
}
