package com.uav.buoy.weather;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.uav.buoy.weather", "com.uav.common"})
@EnableFeignClients(basePackages = "com.uav.common.feign")
public class BuoyWeatherApplication {
    public static void main(String[] args) {
        SpringApplication.run(BuoyWeatherApplication.class, args);
    }
}
