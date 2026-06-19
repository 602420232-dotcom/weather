package com.uav.buoy.weather.repository;

import com.uav.buoy.weather.model.BuoyData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 浮标数据 JPA Repository
 */
@Repository
public interface BuoyDataRepository extends JpaRepository<BuoyData, Long> {

    /** 按浮标编号查询最新数据 */
    @Query("SELECT b FROM BuoyData b WHERE b.buoyId = :buoyId ORDER BY b.collectTime DESC LIMIT 1")
    Optional<BuoyData> findLatestByBuoyId(@Param("buoyId") String buoyId);

    /** 按浮标编号分页查询历史数据 */
    Page<BuoyData> findByBuoyIdOrderByCollectTimeDesc(String buoyId, Pageable pageable);

    /** 查询指定时间范围内的数据 */
    @Query("SELECT b FROM BuoyData b WHERE b.collectTime BETWEEN :start AND :end ORDER BY b.collectTime DESC")
    Page<BuoyData> findByCollectTimeBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}
