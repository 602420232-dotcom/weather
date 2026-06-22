package com.uav.detection.drone.repository;

import com.uav.detection.drone.model.DetectionRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 探测航线仓库
 */
@Repository
public interface DetectionRouteRepository extends JpaRepository<DetectionRoute, Long> {

    /** 按任务ID查询所有航点 (按序号排序) */
    List<DetectionRoute> findByMissionIdOrderBySequenceNumAsc(Long missionId);

    /** 删除指定任务的所有航点 */
    void deleteByMissionId(Long missionId);
}
