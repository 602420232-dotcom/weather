package com.uav.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

/**
 * WRF处理器服务Feign Client
 * 用于声明式调用wrf-processor-service服务
 */
@FeignClient(name = "wrf-processor-service", url = "${services.wrf-processor.url:http://wrf-processor:8081}",
        fallback = WrfProcessorClientFallback.class)
public interface WrfProcessorClient {

    /**
     * 解析WRF数据
     */
    @PostMapping("/api/wrf/parse")
    Map<String, Object> parseWrfData(@RequestBody Map<String, Object> request);

    /**
     * 获取天气数据
     */
    @GetMapping("/api/wrf/data")
    Map<String, Object> getWeatherData(@RequestParam("fileId") String fileId);

    /**
     * 获取统计信息
     */
    @GetMapping("/api/wrf/stats")
    Map<String, Object> getStatistics(@RequestParam("fileId") String fileId);

    /**
     * 上传WRF数据
     */
    @PostMapping("/api/wrf/upload")
    Map<String, Object> uploadWrfData(@RequestBody Map<String, Object> request);

    /**
     * 列出WRF数据
     */
    @GetMapping("/api/wrf/list")
    Map<String, Object> listWrfData(@RequestParam("page") Integer page, @RequestParam("size") Integer size);

    /**
     * 获取WRF数据详情
     */
    @GetMapping("/api/wrf/detail")
    Map<String, Object> getWrfDataDetail(@RequestParam("id") Long id);

    /**
     * 健康检查
     */
    @GetMapping("/actuator/health")
    Map<String, Object> health();
}
