package com.uav.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class PythonAlgorithmUtil {
    
    @Value("${uav.python.script-path}")
    private String scriptPath;
    
    @Value("${uav.python.timeout}")
    private int timeout;
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * 调用Python脚本执行算法
     * @param scriptName 脚本名称
     * @param params 参数映射
     * @return 执行结果
     */
    public String executePythonScript(String scriptName, Map<String, Object> params) {
        try {
            // 构建Python命令
            StringBuilder command = new StringBuilder();
            command.append("python").append(" ");
            command.append(scriptPath).append("/").append(scriptName).append(" ");
            
            // 添加参数
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                command.append("--").append(entry.getKey()).append(" ").append(entry.getValue()).append(" ");
            }
            
            log.info("执行Python脚本: {}", command.toString());
            
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", command.toString());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 使用Future获取结果
            Future<String> future = executorService.submit(() -> {
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                return output.toString();
            });
            
            // 等待执行完成，设置超时
            String result = future.get(timeout, TimeUnit.MILLISECONDS);
            process.waitFor();
            
            log.info("Python脚本执行完成，退出码: {}", process.exitValue());
            return result;
            
        } catch (TimeoutException e) {
            log.error("Python脚本执行超时", e);
            return "{\"error\": \"执行超时\"}";
        } catch (Exception e) {
            log.error("执行Python脚本时发生错误", e);
            return "{\"error\": \"执行错误: " + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 解析WRF气象数据
     * @param filePath WRF文件路径
     * @param height 高度
     * @return 解析结果
     */
    public String parseWRFData(String filePath, int height) {
        Map<String, Object> params = Map.of(
            "file_path", filePath,
            "height", height
        );
        return executePythonScript("wrf/wrf_parser.py", params);
    }
    
    /**
     * 执行贝叶斯同化
     * @param backgroundData 背景场数据
     * @param observationData 观测数据
     * @return 同化结果
     */
    public String performBayesianAssimilation(String backgroundData, String observationData) {
        Map<String, Object> params = Map.of(
            "background", backgroundData,
            "observations", observationData
        );
        return executePythonScript("assimilation/bayesian_assimilation.py", params);
    }
    
    /**
     * 执行气象预测与订正
     * @param wrfData WRF数据
     * @param historicalData 历史数据
     * @return 订正结果
     */
    public String correctMeteorData(String wrfData, String historicalData) {
        Map<String, Object> params = Map.of(
            "wrf_data", wrfData,
            "historical_data", historicalData
        );
        return executePythonScript("prediction/meteor_forecast.py", params);
    }
    
    /**
     * 执行路径规划
     * @param tasks 任务数据
     * @param drones 无人机数据
     * @param weatherData 气象数据
     * @return 规划结果
     */
    public String planPath(String tasks, String drones, String weatherData) {
        Map<String, Object> params = Map.of(
            "tasks", tasks,
            "drones", drones,
            "weather_data", weatherData
        );
        return executePythonScript("path-planning/three_layer_planner.py", params);
    }
    
    /**
     * 关闭线程池
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}