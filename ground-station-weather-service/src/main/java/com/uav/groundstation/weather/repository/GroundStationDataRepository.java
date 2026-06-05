package com.uav.groundstation.weather.repository;

import com.uav.groundstation.weather.model.GroundStationData;
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
 * 地面气象站数据仓库
 */
@Repository
public interface GroundStationDataRepository extends JpaRepository<GroundStationData, Long> {

    /**
     * 查询指定站点最新一条数据
     */
    @Query("SELECT g FROM GroundStationData g WHERE g.stationId = :stationId ORDER BY g.collectTime DESC")
    Optional<GroundStationData> findLatestByStationId(@Param("stationId") String stationId);

    /**
     * 按站点ID分页查询历史数据
     */
    Page<GroundStationData> findByStationIdOrderByCollectTimeDesc(String stationId, Pageable pageable);

    /**
     * 按时间范围查询
     */
    List<GroundStationData> findByCollectTimeBetween(LocalDateTime start, LocalDateTime end);
}
