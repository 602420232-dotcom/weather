package com.uav.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PathPlanningRequest {

    @NotBlank(message = "规划算法不能为空")
    @Size(max = 50, message = "算法名称最长50字符")
    private String algorithm;

    private Map<String, Object> drones;

    private Map<String, Object> tasks;

    private Map<String, Object> weatherData;

    private Map<String, Object> constraints;
}
