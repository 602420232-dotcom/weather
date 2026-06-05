package com.uav.radiosonde.weather.repository;

import com.uav.radiosonde.weather.model.RadiosondeData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 探空数据仓库
 */
@Repository
public interface RadiosondeDataRepository extends JpaRepository<RadiosondeData, Long> {

    /**
     * 查询指定站点最新一次探空的所有气压层数据
     */
    @Query("SELECT r FROM RadiosondeData r WHERE r.stationId = :stationId " +
            "AND r.launchTime = (SELECT MAX(r2.launchTime) FROM RadiosondeData r2 WHERE r2.stationId = :stationId) " +
            "ORDER BY r.pressureLevel DESC")
    List<RadiosondeData> findLatestProfileByStationId(@Param("stationId") String stationId);

    /**
     * 查询指定站点、指定气压层的最新数据
     */
    @Query("SELECT r FROM RadiosondeData r WHERE r.stationId = :stationId AND r.pressureLevel = :level " +
            "ORDER BY r.launchTime DESC")
    Page<RadiosondeData> findByStationIdAndLevel(@Param("stationId") String stationId,
                                                  @Param("level") Integer level,
                                                  Pageable pageable);

    /**
     * 按释放时间查询所有站点的探空数据
     */
    List<RadiosondeData> findByLaunchTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 获取所有唯一探空站
     */
    @Query("SELECT DISTINCT r.stationId, r.stationName, r.longitude, r.latitude, r.stationAltitude " +
            "FROM RadiosondeData r ORDER BY r.stationId")
    List<Object[]> findDistinctStations();
}
