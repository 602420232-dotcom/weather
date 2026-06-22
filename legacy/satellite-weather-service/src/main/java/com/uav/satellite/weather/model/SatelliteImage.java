package com.uav.satellite.weather.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * 卫星云图数据实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "satellite_image", indexes = {
        @Index(name = "idx_region", columnList = "region"),
        @Index(name = "idx_channel", columnList = "channel"),
        @Index(name = "idx_capture_time", columnList = "captureTime")
})
public class SatelliteImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 卫星标识 */
    @Column(nullable = false, length = 64)
    private String satelliteId;

    /** 卫星名称 */
    @Column(length = 128)
    private String satelliteName;

    /** 区域范围(如 CHINA, ASIA, GLOBAL) */
    @Column(nullable = false, length = 32)
    private String region;

    /** 波段通道(如 IR=红外, VIS=可见光, WV=水汽) */
    @Column(nullable = false, length = 16)
    private String channel;

    /** 云图文件URL */
    @Column(length = 512)
    private String imageUrl;

    /** 左上角经度 */
    private Double topLeftLon;

    /** 左上角纬度 */
    private Double topLeftLat;

    /** 右下角经度 */
    private Double bottomRightLon;

    /** 右下角纬度 */
    private Double bottomRightLat;

    /** 分辨率(像素/度) */
    private Double resolution;

    /** 拍摄时间 */
    @Column(nullable = false)
    private LocalDateTime captureTime;

    /** 记录创建时间 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
