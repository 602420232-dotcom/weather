package com.uav.detection.drone.repository;

import com.uav.detection.drone.model.DetectionMission;
import com.uav.detection.drone.model.MissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 探测任务仓库
 */
@Repository
public interface DetectionMissionRepository extends JpaRepository<DetectionMission, Long> {

    /** 按状态查询 */
    List<DetectionMission> findByStatus(MissionStatus status);

    /** 按无人机ID和状态查询 */
    Optional<DetectionMission> findByDroneIdAndStatus(String droneId, MissionStatus status);

    /** 查询指定无人机正在执行的任务 (IN_FLIGHT) */
    @Query("SELECT m FROM DetectionMission m WHERE m.droneId = :droneId AND m.status = 'IN_FLIGHT'")
    Optional<DetectionMission> findActiveMissionByDroneId(@Param("droneId") String droneId);

    /** 查询已着陆待上传的任务 */
    List<DetectionMission> findByStatusAndDataOfflineTrue(MissionStatus status);

    /** 分页查询所有任务 (按创建时间倒序) */
    Page<DetectionMission> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
