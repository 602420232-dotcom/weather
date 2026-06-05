package com.uav.satellite.weather.repository;

import com.uav.satellite.weather.model.SatelliteImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 卫星云图数据仓库
 */
@Repository
public interface SatelliteImageRepository extends JpaRepository<SatelliteImage, Long> {

    /**
     * 按区域和通道查询最新一张云图
     */
    @Query("SELECT s FROM SatelliteImage s WHERE s.region = :region AND s.channel = :channel ORDER BY s.captureTime DESC")
    Optional<SatelliteImage> findLatestByRegionAndChannel(@Param("region") String region, @Param("channel") String channel);

    /**
     * 按区域和通道分页查询历史云图
     */
    Page<SatelliteImage> findByRegionAndChannelOrderByCaptureTimeDesc(String region, String channel, Pageable pageable);

    /**
     * 按时间范围查询
     */
    List<SatelliteImage> findByCaptureTimeBetween(LocalDateTime start, LocalDateTime end);
}
