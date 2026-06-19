package com.uav.detection.drone.repository;

import com.uav.detection.drone.model.DetectionSample;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 探测采样数据仓库
 */
@Repository
public interface DetectionSampleRepository extends JpaRepository<DetectionSample, Long> {

    /** 按任务ID查询所有样本 (按采样时间排序) */
    List<DetectionSample> findByMissionIdOrderBySampleTimeAsc(Long missionId);

    /** 按任务ID分页查询 */
    Page<DetectionSample> findByMissionId(Long missionId, Pageable pageable);

    /** 查询指定任务的最大采样序号 */
    @Query("SELECT COALESCE(MAX(s.sequenceNum), 0) FROM DetectionSample s WHERE s.missionId = :missionId")
    Integer findMaxSequenceNumByMissionId(@Param("missionId") Long missionId);

    /** 按高度范围查询样本 */
    List<DetectionSample> findByMissionIdAndAltitudeBetween(Long missionId, Double minAlt, Double maxAlt);

    /** 按时间范围查询所有无人机样本 (用于数据同化输入) */
    List<DetectionSample> findBySampleTimeBetweenOrderBySampleTimeAsc(LocalDateTime start, LocalDateTime end);

    /** 查询任务的数据统计 */
    @Query("SELECT s.missionId, COUNT(s), MIN(s.sampleTime), MAX(s.sampleTime), " +
            "MIN(s.altitude), MAX(s.altitude), AVG(s.temperature), AVG(s.humidity), AVG(s.windSpeed) " +
            "FROM DetectionSample s WHERE s.missionId = :missionId GROUP BY s.missionId")
    List<Object[]> getMissionStatistics(@Param("missionId") Long missionId);
}
