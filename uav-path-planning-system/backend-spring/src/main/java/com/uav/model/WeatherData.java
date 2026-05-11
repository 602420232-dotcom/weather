package com.uav.model;
import lombok.Data;
import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Data
@Entity
@Table(name = "weather_data")
public class WeatherData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private LocalDateTime forecastTime;
    private LocalDateTime updateTime;
    
    private Double minLatitude;
    private Double maxLatitude;
    private Double minLongitude;
    private Double maxLongitude;
    private Integer height;
    
    private String windSpeedData;
    private String windDirData;
    private String temperatureData;
    private String humidityData;
    private String turbulenceData;
    private String visibilityData;
    private String thunderRiskData;
    
    private String uncertaintyData;
    private String varianceField;
    
    private String source;
    private String status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    private Double latitude;
    private Double longitude;
    private Double temperature;
    private Double windSpeed;
    private Integer windDirection;
    private Integer humidity;
    private Double pressure;
}