package com.meteor.forecast.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {
    
    @Value("${forecast.python-script}")
    private String pythonScriptPath;
    
    @Value("${forecast.timeout}")
    private long timeout;
    
    /**
     * 执行气象预测
     * @param request 预测请求
     * @return 预测结果
     */
    @PostMapping("/predict")
    public Map<String, Object> predict(@RequestBody Map<String, Object> request) {
        try {
            // 提取输入数据
            Object inputData = request.get("data");
            if (inputData == null) {
                return Map.of(
                    "success", false,
                    "error", "缺少输入数据"
                );
            }
            
            // 执行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3", pythonScriptPath, "predict", inputData.toString()
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
     * 执行气象数据订正
     * @param request 订正请求
     * @return 订正结果
     */
    @PostMapping("/correct")
    public Map<String, Object> correct(@RequestBody Map<String, Object> request) {
        try {
            // 提取数据
            Object forecastData = request.get("forecastData");
            Object observedData = request.get("observedData");
            
            if (forecastData == null || observedData == null) {
                return Map.of(
                    "success", false,
                    "error", "缺少预测数据或观测数据"
                );
            }
            
            // 执行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3", pythonScriptPath, "correct", forecastData.toString(), observedData.toString()
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
     * 获取可用模型
     * @return 模型列表
     */
    @GetMapping("/models")
    public Map<String, Object> getModels() {
        // 这里应该从文件系统或数据库获取模型信息
        return Map.of(
            "success", true,
            "data", Map.of(
                "lstm_model", "LSTM时间序列预测模型",
                "xgb_model", "XGBoost数据订正模型"
            )
        );
    }
}