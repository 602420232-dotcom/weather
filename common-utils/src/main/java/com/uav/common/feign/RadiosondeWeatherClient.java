package com.uav.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

/**
 * 探空气球气象服务Feign Client
 * 用于声明式调用探空气球数据服务
 */
@FeignClient(name = "radiosonde-weather-service",
        url = "${services.radiosonde-weather.url:http://radiosonde-weather:8091}",
        fallback = RadiosondeWeatherClientFallback.class)
public interface RadiosondeWeatherClient {

    /**
     * 获取探空数据（按站号和气压层查询）
     *
     * @param stationId 探空站编号
     * @param level      气压层(hPa)，可选
     * @return 探空数据
     */
    @GetMapping("/api/radiosonde/data")
    Map<String, Object> getSoundingData(@RequestParam(value = "stationId") String stationId,
                                        @RequestParam(value = "level", required = false) Integer level);

    /**
     * 获取探空站列表
     *
     * @param page 页码
     * @param size 每页数量
     * @return 探空站列表
     */
    @GetMapping("/api/radiosonde/list")
    Map<String, Object> listStations(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                     @RequestParam(value = "size", defaultValue = "10") Integer size);

    /**
     * 获取探空数据详情
     *
     * @param id 探空数据ID
     * @return 探空数据详情
     */
    @GetMapping("/api/radiosonde/{id}")
    Map<String, Object> getSoundingDetail(@PathVariable("id") Long id);

    /**
     * 获取斜温图数据
     *
     * @param id 探空数据ID
     * @return 斜温图数据（含温度曲线、露点曲线、风羽数据）
     */
    @GetMapping("/api/radiosonde/skew-t/{id}")
    Map<String, Object> getSkewTData(@PathVariable("id") Long id);

    /**
     * 上传探空数据
     *
     * @param request 探空数据请求
     * @return 上传结果
     */
    @PostMapping("/api/radiosonde/upload")
    Map<String, Object> uploadSoundingData(@RequestBody Map<String, Object> request);

    /**
     * 健康检查
     */
    @GetMapping("/actuator/health")
    Map<String, Object> health();
}
