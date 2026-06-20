package com.uav.buoy.weather.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 浮标气象数据实体
 * 存储浮标采集的气象观测数据
 */
@Entity
@Table(name = "buoy_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuoyData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 浮标编号 */
    @Column(name = "buoy_id", nullable = false, length = 64)
    private String buoyId;

    /** 浮标名称 */
    @Column(name = "buoy_name", length = 128)
    private String buoyName;

    /** 经度 */
    @Column(name = "longitude")
    private Double longitude;

    /** 纬度 */
    @Column(name = "latitude")
    private Double latitude;

    /** 风速 (m/s) */
    @Column(name = "wind_speed")
    private Double windSpeed;

    /** 风向 (度) */
    @Column(name = "wind_direction")
    private Double windDirection;

    /** 气温 (°C) */
    @Column(name = "temperature")
    private Double temperature;

    /** 气压 (hPa) */
    @Column(name = "pressure")
    private Double pressure;

    /** 湿度 (%) */
    @Column(name = "humidity")
    private Double humidity;

    /** 波高 (m) */
    @Column(name = "wave_height")
    private Double waveHeight;

    /** 水温 (°C) */
    @Column(name = "water_temperature")
    private Double waterTemperature;

    /** 采集时间 */
    @Column(name = "collect_time", nullable = false)
    private LocalDateTime collectTime;

    /** 记录创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
