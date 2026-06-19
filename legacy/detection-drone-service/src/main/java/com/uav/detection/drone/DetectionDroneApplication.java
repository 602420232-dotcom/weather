package com.uav.detection.drone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 探测无人机服务 - 主启动类
 */
@SpringBootApplication(scanBasePackages = {"com.uav.detection.drone", "com.uav.common"})
@EnableFeignClients(basePackages = "com.uav.common.feign")
public class DetectionDroneApplication {

    public static void main(String[] args) {
        SpringApplication.run(DetectionDroneApplication.class, args);
    }
}
