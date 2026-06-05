package com.uav.detection.drone.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * 探测航线航点实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "detection_route", indexes = {
        @Index(name = "idx_mission_route", columnList = "missionId,sequenceNum")
})
public class DetectionRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属任务ID */
    @Column(nullable = false)
    private Long missionId;

    /** 航点序号 */
    @Column(nullable = false)
    private Integer sequenceNum;

    /** 航点经度 */
    @Column(nullable = false)
    private Double longitude;

    /** 航点纬度 */
    @Column(nullable = false)
    private Double latitude;

    /** 航点高度(米) */
    @Column(nullable = false)
    private Double altitude;

    /** 到达该航点时的期望速度(m/s) */
    private Double speed;

    /** 在该航点的悬停时间(秒，用于定点采集) */
    @Builder.Default
    private Integer hoverSeconds = 0;

    /** 航点备注 */
    @Column(length = 256)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
