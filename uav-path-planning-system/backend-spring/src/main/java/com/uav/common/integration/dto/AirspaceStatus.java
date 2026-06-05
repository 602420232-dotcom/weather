package com.uav.common.integration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 空域状态DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirspaceStatus {

    /**
     * 空域区域ID
     */
    private String zoneId;

    /**
     * 空域状态
     */
    private Status status;

    /**
     * 限制条件
     */
    private String restrictions;

    /**
     * 天气预警
     */
    private String weatherAlert;

    /**
     * 当前无人机数量
     */
    private Integer droneCount;

    /**
     * 最大允许无人机数量
     */
    private Integer maxDronesAllowed;

    /**
     * 空域类型
     */
    private AirspaceType airspaceType;

    /**
     * 空域状态枚举
     */
    public enum Status {
        ACTIVE,         // 活跃
        RESTRICTED,     // 限制
        CLOSED,         // 关闭
        EMERGENCY       // 紧急状态
    }

    /**
     * 空域类型枚举
     */
    public enum AirspaceType {
        CONTROLLED,     // 管制空域
        UNCONTROLLED,   // 非管制空域
        PROHIBITED,     // 禁飞区
        RESTRICTED      // 限制区
    }
}
