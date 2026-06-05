package com.uav.detection.drone.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * 探测采样数据实体
 * 存储探测无人机飞行过程中采集的气象样本数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "detection_sample", indexes = {
        @Index(name = "idx_mission_id", columnList = "missionId"),
        @Index(name = "idx_sample_time", columnList = "sampleTime"),
        @Index(name = "idx_altitude", columnList = "altitude")
})
public class DetectionSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属任务ID */
    @Column(nullable = false)
    private Long missionId;

    /** 采集无人机 ID */
    @Column(nullable = false, length = 64)
    private String droneId;

    /** 采样序号 (任务内递增) */
    @Column(nullable = false)
    private Integer sequenceNum;

    /** 采样时间 */
    @Column(nullable = false)
    private LocalDateTime sampleTime;

    /** 采样点经度 */
    @Column(nullable = false)
    private Double longitude;

    /** 采样点纬度 */
    @Column(nullable = false)
    private Double latitude;

    /** 采样点高度(米) */
    @Column(nullable = false)
    private Double altitude;

    /** 温度(摄氏度) */
    private Double temperature;

    /** 相对湿度(%) */
    private Double humidity;

    /** 气压(hPa) */
    private Double pressure;

    /** 风速(m/s) */
    private Double windSpeed;

    /** 风向(度) */
    private Double windDirection;

    /** 阵风(m/s) */
    private Double windGust;

    /** 能见度(km) */
    private Double visibility;

    /** CO2浓度(ppm，可选) */
    private Double co2;

    /** PM2.5浓度(ug/m3，可选) */
    private Double pm25;

    /** 数据质量标记 (0-1) */
    @Builder.Default
    private Double qualityFlag = 1.0;

    /** 是否来自离线缓存 */
    @Builder.Default
    private Boolean fromOffline = false;

    /** 记录创建时间 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
