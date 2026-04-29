package com.path.planning.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

@RestController
@RequestMapping("/api/planning")
public class PlanningController {
    
    @Value("${planning.python-script}")
    private String pythonScriptPath;
    
    @Value("${planning.timeout}")
    private long timeout;
    
    /**
     * 执行VRPTW任务调度
     * @param request 调度请求
     * @return 调度结果
     */
    @PostMapping("/vrptw")
    public Map<String, Object> vrptw(@RequestBody Map<String, Object> request) {
        try {
            // 执行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3", pythonScriptPath, "vrptw", request.toString()
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
     * 执行A*全局路径规划
     * @param request 规划请求
     * @return 规划结果
     */
    @PostMapping("/astar")
    public Map<String, Object> astar(@RequestBody Map<String, Object> request) {
        try {
            // 执行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3", pythonScriptPath, "astar", request.toString()
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
     * 执行DWA实时避障
     * @param request 避障请求
     * @return 避障结果
     */
    @PostMapping("/dwa")
    public Map<String, Object> dwa(@RequestBody Map<String, Object> request) {
        try {
            // 执行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3", pythonScriptPath, "dwa", request.toString()
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
     * 执行完整路径规划
     * @param request 规划请求
     * @return 规划结果
     */
    @PostMapping("/full")
    public Map<String, Object> full(@RequestBody Map<String, Object> request) {
        try {
            // 执行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3", pythonScriptPath, "full", request.toString()
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