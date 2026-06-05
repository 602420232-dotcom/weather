package com.uav.detection.drone.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * 探测任务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "detection_mission", indexes = {
        @Index(name = "idx_mission_status", columnList = "status"),
        @Index(name = "idx_drone_id", columnList = "droneId"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
public class DetectionMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务名称 */
    @Column(nullable = false, length = 128)
    private String missionName;

    /** 任务类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MissionType missionType;

    /** 任务状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private MissionStatus status = MissionStatus.CREATED;

    /** 分配的无人机 ID */
    @Column(length = 64)
    private String droneId;

    /** 无人机名称 */
    @Column(length = 128)
    private String droneName;

    /** 目标区域描述 */
    @Column(columnDefinition = "TEXT")
    private String targetAreaDesc;

    /** 目标区域西南角经度 */
    private Double areaMinLon;

    /** 目标区域西南角纬度 */
    private Double areaMinLat;

    /** 目标区域东北角经度 */
    private Double areaMaxLon;

    /** 目标区域东北角纬度 */
    private Double areaMaxLat;

    /** 最低飞行高度(米) */
    private Double minAltitude;

    /** 最高飞行高度(米) */
    private Double maxAltitude;

    /** 格网分辨率(米，适用于 GRID_SCAN) */
    private Double gridResolution;

    /** 垂直层数(适用于 VERTICAL_PROFILE) */
    private Integer verticalLayers;

    /** 跟踪目标标识(适用于 TARGET_TRACKING) */
    @Column(length = 128)
    private String trackingTarget;

    /** 计划开始时间 */
    private LocalDateTime scheduledStart;

    /** 实际开始时间 */
    private LocalDateTime actualStart;

    /** 实际结束时间 */
    private LocalDateTime actualEnd;

    /** 采集样本总数 */
    @Builder.Default
    private Integer sampleCount = 0;

    /** 数据是否仍为离线状态 */
    @Builder.Default
    private Boolean dataOffline = false;

    /** 任务备注 */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
