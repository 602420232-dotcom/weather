package com.uav.common.integration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 飞行审批DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightApproval {

    /**
     * 飞行计划ID
     */
    private String planId;

    /**
     * 是否批准
     */
    private boolean approved;

    /**
     * 批准时间
     */
    private LocalDateTime approvedAt;

    /**
     * 过期时间
     */
    private LocalDateTime expirationTime;

    /**
     * 限制条件
     */
    private String restrictions;

    /**
     * 审批机构
     */
    private String approvedBy;

    /**
     * 拒绝原因(如果未批准)
     */
    private String rejectionReason;

    /**
     * 审批编号
     */
    private String approvalNumber;
}
