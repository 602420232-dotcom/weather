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
@Table(name = "drones")
public class Drone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String model;
    private String serialNumber;
    
    private Double maxSpeed;
    private Double maxCapacity;
    private Double maxBattery;
    private Double cruiseSpeed;
    private Double windResistance;
    
    private String status;
    
    private Double currentLatitude;
    private Double currentLongitude;
    private Double currentAltitude;
    
    private Integer batteryLevel;
    
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
    private Double altitude;
    private Double speed;
    private Integer battery;
}