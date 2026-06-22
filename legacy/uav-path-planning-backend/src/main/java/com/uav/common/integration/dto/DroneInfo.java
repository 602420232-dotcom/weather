package com.uav.common.integration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 无人机信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DroneInfo {

    /**
     * 无人机ID
     */
    private String droneId;

    /**
     * 型号
     */
    private String model;

    /**
     * 制造商
     */
    private String manufacturer;

    /**
     * 序列号
     */
    private String serialNumber;

    /**
     * 操作员名称
     */
    private String operatorName;

    /**
     * 操作员联系方式
     */
    private String operatorContact;

    /**
     * 最大起飞重量(kg)
     */
    private Double maxTakeoffWeight;

    /**
     * 最大飞行高度(m)
     */
    private Integer maxAltitude;

    /**
     * 最大飞行速度(m/s)
     */
    private Double maxSpeed;

    /**
     * 续航时间(min)
     */
    private Integer enduranceMinutes;

    /**
     * 注册状态
     */
    private RegistrationStatus registrationStatus;

    public enum RegistrationStatus {
        PENDING,    // 待审核
        APPROVED,   // 已批准
        REJECTED,   // 已拒绝
        EXPIRED     // 已过期
    }
}
