package com.uav.common.integration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 飞行计划DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightPlan {

    /**
     * 飞行计划ID
     */
    private String planId;

    /**
     * 无人机ID
     */
    private String droneId;

    /**
     * 任务类型
     */
    private String missionType;

    /**
     * 起飞时间
     */
    private LocalDateTime takeoffTime;

    /**
     * 降落时间
     */
    private LocalDateTime landingTime;

    /**
     * 起飞点纬度
     */
    private Double takeoffLat;

    /**
     * 起飞点经度
     */
    private Double takeoffLon;

    /**
     * 起飞点高度(m)
     */
    private Double takeoffAltitude;

    /**
     * 降落点纬度
     */
    private Double landingLat;

    /**
     * 降落点经度
     */
    private Double landingLon;

    /**
     * 降落点高度(m)
     */
    private Double landingAltitude;

    /**
     * 最大飞行高度(m)
     */
    private Integer maxAltitude;

    /**
     * 航点列表
     */
    private List<Waypoint> waypoints;

    /**
     * 备注信息
     */
    private String remarks;

    /**
     * 航点
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Waypoint {
        private Double lat;
        private Double lon;
        private Double altitude;
        private Integer sequence;
        private String action;
    }
}
