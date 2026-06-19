package com.uav.common.script;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class PythonScriptInvoker {

    public Map<String, Object> executeAsMap(String scriptPath, String functionName, Map<String, Object> params) {
        log.warn("PythonScriptInvoker: 模拟执行脚本 {} 的函数 {}", scriptPath, functionName);
        log.warn("参数: {}", params);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "模拟执行成功");
        result.put("data", params);
        
        return result;
    }
}
