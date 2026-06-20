package com.uav.radiosonde.weather;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 探空气球气象数据服务 - 主启动类
 */
@SpringBootApplication(scanBasePackages = {"com.uav.radiosonde.weather", "com.uav.common"})
@EnableFeignClients(basePackages = "com.uav.common.feign")
public class RadiosondeWeatherApplication {

    public static void main(String[] args) {
        SpringApplication.run(RadiosondeWeatherApplication.class, args);
    }
}
