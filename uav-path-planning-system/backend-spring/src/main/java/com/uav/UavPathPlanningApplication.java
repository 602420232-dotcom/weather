package com.uav;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableFeignClients(basePackages = "com.uav.common.feign")
public class UavPathPlanningApplication {
    public static void main(String[] args) {
        SpringApplication.run(UavPathPlanningApplication.class, args);
    }
}