package com.uav.radiosonde.weather.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * 探空气球观测数据实体
 * 存储各标准气压层的温度、露点、风场等垂直廓线数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "radiosonde_data", indexes = {
        @Index(name = "idx_station_launch", columnList = "stationId,launchTime"),
        @Index(name = "idx_pressure_level", columnList = "pressureLevel")
})
public class RadiosondeData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** WMO探空站编号（如 50527=海拉尔, 54511=北京） */
    @Column(nullable = false, length = 16)
    private String stationId;

    /** 探空站名称 */
    @Column(nullable = false, length = 128)
    private String stationName;

    /** 探空站经度 */
    @Column(nullable = false)
    private Double longitude;

    /** 探空站纬度 */
    @Column(nullable = false)
    private Double latitude;

    /** 探空站海拔高度(米) */
    private Double stationAltitude;

    /** 气球释放时间 */
    @Column(nullable = false)
    private LocalDateTime launchTime;

    /** 气压层(hPa)，标准层: 1000/925/850/700/500/400/300/250/200/150/100 */
    @Column(nullable = false)
    private Integer pressureLevel;

    /** 位势高度(米) */
    private Double geopotentialHeight;

    /** 温度(摄氏度) */
    private Double temperature;

    /** 露点温度(摄氏度) */
    private Double dewPoint;

    /** 相对湿度(%) */
    private Double relativeHumidity;

    /** 风速(m/s) */
    private Double windSpeed;

    /** 风向(度) */
    private Double windDirection;

    /** 混合比(g/kg) */
    private Double mixingRatio;

    /** 位温(K)，用于分析大气稳定度 */
    private Double potentialTemperature;

    /** 假相当位温(K)，用于分析对流不稳定 */
    private Double equivalentPotentialTemperature;

    /** 数据来源（IGRA/UWYO/CMA/本地上传） */
    @Column(length = 32)
    private String dataSource;

    /** 数据质量标记 (0-1) */
    @Builder.Default
    private Double qualityFlag = 1.0;

    /** 记录创建时间 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
