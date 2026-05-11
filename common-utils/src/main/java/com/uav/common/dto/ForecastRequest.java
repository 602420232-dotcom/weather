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
public class ForecastRequest {

    @NotBlank(message = "预测方法不能为空")
    @Size(max = 50, message = "方法名称最长50字符")
    private String method;

    private Map<String, Object> data;

    private Map<String, Object> config;
}
