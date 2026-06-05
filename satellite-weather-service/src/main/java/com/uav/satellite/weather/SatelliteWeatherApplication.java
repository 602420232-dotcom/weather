package com.uav.satellite.weather;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 卫星气象数据服务 - 主启动类
 */
@SpringBootApplication(scanBasePackages = {"com.uav.satellite.weather", "com.uav.common"})
@EnableFeignClients(basePackages = "com.uav.common.feign")
public class SatelliteWeatherApplication {

    public static void main(String[] args) {
        SpringApplication.run(SatelliteWeatherApplication.class, args);
    }
}
