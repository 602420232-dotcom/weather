package com.uav.common.integration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 紧急响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyResponse {

    /**
     * 报警ID
     */
    private String alertId;

    /**
     * 无人机ID
     */
    private String droneId;

    /**
     * 紧急情况类型
     */
    private String type;

    /**
     * 严重程度
     */
    private String severity;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 处理指令
     */
    private String instructions;

    /**
     * 处理状态
     */
    private ResponseStatus responseStatus;

    /**
     * 处理人员
     */
    private String handledBy;

    /**
     * 响应状态枚举
     */
    public enum ResponseStatus {
        PENDING,        // 待处理
        ACKNOWLEDGED,   // 已确认
        IN_PROGRESS,    // 处理中
        RESOLVED,       // 已解决
        CLOSED          // 已关闭
    }
}
