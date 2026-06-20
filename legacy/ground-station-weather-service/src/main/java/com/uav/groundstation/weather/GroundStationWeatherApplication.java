package com.uav.groundstation.weather;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 地面气象站数据服务 - 主启动类
 */
@SpringBootApplication(scanBasePackages = {"com.uav.groundstation.weather", "com.uav.common"})
@EnableFeignClients(basePackages = "com.uav.common.feign")
public class GroundStationWeatherApplication {

    public static void main(String[] args) {
        SpringApplication.run(GroundStationWeatherApplication.class, args);
    }
}
