package com.assimilation.service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

@RestController
@RequestMapping("/api/assimilation")
public class AssimilationController {
    
    @Value("${assimilation.python-script}")
    private String pythonScriptPath;
    
    @Value("${assimilation.timeout}")
    private long timeout;
    
    /**
     * 执行贝叶斯同化
     * @param request 同化请求
     * @return 同化结果
     */
    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestBody Map<String, Object> request) {
        try {
            // 执行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3", pythonScriptPath, "execute", request.toString()
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            int exitCode = process.waitFor();
            
            return Map.of(
                "success", true,
                "data", output.toString()
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 获取方差场
     * @param request 方差场请求
     * @return 方差场结果
     */
    @PostMapping("/variance")
    public Map<String, Object> getVariance(@RequestBody Map<String, Object> request) {
        try {
            // 执行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3", pythonScriptPath, "variance", request.toString()
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            int exitCode = process.waitFor();
            
            return Map.of(
                "success", true,
                "data", output.toString()
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 批量处理
     * @param request 批量处理请求
     * @return 批量处理结果
     */
    @PostMapping("/batch")
    public Map<String, Object> batchProcess(@RequestBody Map<String, Object> request) {
        try {
            // 执行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3", pythonScriptPath, "batch", request.toString()
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            int exitCode = process.waitFor();
            
            return Map.of(
                "success", true,
                "data", output.toString()
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
}