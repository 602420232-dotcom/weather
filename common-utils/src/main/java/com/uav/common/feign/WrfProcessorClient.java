package com.uav.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
     * 上传WRF数据
     */
    @PostMapping("/api/wrf/upload")
    Map<String, Object> uploadWrfData(@RequestBody Map<String, Object> request);

    /**
     * 获取WRF数据列表
     */
    @GetMapping("/api/wrf/list")
    Map<String, Object> listWrfData(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                     @RequestParam(value = "size", defaultValue = "10") Integer size);

    /**
     * 获取WRF数据详情
     */
    @GetMapping("/api/wrf/{id}")
    Map<String, Object> getWrfDataDetail(@PathVariable("id") Long id);

    /**
     * 解析WRF数据
     */
    @PostMapping("/api/wrf/parse")
    Map<String, Object> parseWrfData(@RequestBody Map<String, Object> request);

    /**
     * 健康检查
     */
    @GetMapping("/actuator/health")
    Map<String, Object> health();
}
