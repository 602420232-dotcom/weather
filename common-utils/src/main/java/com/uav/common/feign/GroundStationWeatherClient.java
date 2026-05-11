package com.uav.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

/**
 * 地面气象站服务Feign Client
 * 用于声明式调用地面气象站数据服务
 */
@FeignClient(name = "ground-station-weather-service", url = "${services.ground-station-weather.url:http://ground-station-weather:8083}",
        fallback = GroundStationWeatherClientFallback.class)
public interface GroundStationWeatherClient {

    /**
     * 获取地面气象站数据
     *
     * @param stationId 站点ID
     * @return 地面气象站数据
     */
    @GetMapping("/api/ground-station/data")
    Map<String, Object> getStationData(@RequestParam(value = "stationId", required = false) String stationId);

    /**
     * 获取地面气象站列表
     * 
     * @param page 页码
     * @param size 每页数量
     * @return 气象站列表
     */
    @GetMapping("/api/ground-station/list")
    Map<String, Object> listStations(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                     @RequestParam(value = "size", defaultValue = "10") Integer size);

    /**
     * 获取气象站详情
     * 
     * @param id 气象站ID
     * @return 气象站详情
     */
    @GetMapping("/api/ground-station/{id}")
    Map<String, Object> getStationDetail(@PathVariable("id") Long id);

    /**
     * 获取气象站实时数据
     * 
     * @param stationId 站点ID
     * @return 实时数据
     */
    @GetMapping("/api/ground-station/realtime")
    Map<String, Object> getRealtimeData(@RequestParam(value = "stationId") String stationId);

    /**
     * 上传气象站数据
     * 
     * @param request 气象站数据请求
     * @return 上传结果
     */
    @PostMapping("/api/ground-station/upload")
    Map<String, Object> uploadStationData(@RequestBody Map<String, Object> request);

    /**
     * 健康检查
     */
    @GetMapping("/actuator/health")
    Map<String, Object> health();
}
