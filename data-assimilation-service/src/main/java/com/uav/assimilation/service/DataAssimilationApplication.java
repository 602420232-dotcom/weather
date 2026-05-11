package com.uav.assimilation.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.uav.common.feign")
public class DataAssimilationApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataAssimilationApplication.class, args);
    }
}