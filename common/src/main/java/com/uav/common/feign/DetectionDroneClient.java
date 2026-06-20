package com.uav.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
/**
 * 探测无人机服务Feign Client
 */
@FeignClient(name = "detection-drone-service",
        url = "${services.detection-drone.url:http://detection-drone:8092}",
        fallback = DetectionDroneClientFallback.class)
public interface DetectionDroneClient {

    /** 创建探测任务 */
    @PostMapping("/api/detection/mission/create")
    Map<String, Object> createMission(@RequestBody Map<String, Object> request);

    /** 查询任务列表 */
    @GetMapping("/api/detection/mission/list")
    Map<String, Object> listMissions(@RequestParam("page") Integer page,
                                     @RequestParam("size") Integer size,
                                     @RequestParam(value = "status", required = false) String status);

    /** 获取任务状态 */
    @GetMapping("/api/detection/mission/{id}/status")
    Map<String, Object> getMissionStatus(@PathVariable("id") Long id);

    /** 更新任务状态 */
    @PutMapping("/api/detection/mission/{id}/status")
    Map<String, Object> updateMissionStatus(@PathVariable("id") Long id,
                                            @RequestBody Map<String, Object> request);

    /** 获取任务全部采集数据 */
    @GetMapping("/api/detection/mission/{id}/data")
    Map<String, Object> getMissionData(@PathVariable("id") Long id,
                                       @RequestParam(value = "page", defaultValue = "1") Integer page,
                                       @RequestParam(value = "size", defaultValue = "100") Integer size);

    /** 上传采样数据 */
    @PostMapping("/api/detection/sample/upload")
    Map<String, Object> uploadSample(@RequestBody Map<String, Object> request);

    /** 查询历史采集数据 */
    @GetMapping("/api/detection/sample/history")
    Map<String, Object> getSampleHistory(@RequestParam("droneId") String droneId,
                                         @RequestParam(value = "hours", defaultValue = "24") Integer hours);

    /** 获取垂直剖面 */
    @GetMapping("/api/detection/mission/{id}/vertical-profile")
    Map<String, Object> getVerticalProfile(@PathVariable("id") Long id,
                                           @RequestParam(value = "layerSize", defaultValue = "50") Double layerSize);

    /** 健康检查 */
    @GetMapping("/actuator/health")
    Map<String, Object> health();
}
