package com.uav.groundstation.weather.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * 地面气象站观测数据实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ground_station_data", indexes = {
        @Index(name = "idx_station_id", columnList = "stationId"),
        @Index(name = "idx_collect_time", columnList = "collectTime")
})
public class GroundStationData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 站点编号 */
    @Column(nullable = false, length = 64)
    private String stationId;

    /** 站点名称 */
    @Column(nullable = false, length = 128)
    private String stationName;

    /** 经度 */
    @Column(nullable = false)
    private Double longitude;

    /** 纬度 */
    @Column(nullable = false)
    private Double latitude;

    /** 海拔高度(米) */
    private Double altitude;

    /** 风速(m/s) */
    private Double windSpeed;

    /** 风向(度) */
    private Double windDirection;

    /** 温度(摄氏度) */
    private Double temperature;

    /** 气压(hPa) */
    private Double pressure;

    /** 相对湿度(%) */
    private Double humidity;

    /** 降水量(mm) */
    private Double precipitation;

    /** 能见度(km) */
    private Double visibility;

    /** 采集时间 */
    @Column(nullable = false)
    private LocalDateTime collectTime;

    /** 记录创建时间 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
