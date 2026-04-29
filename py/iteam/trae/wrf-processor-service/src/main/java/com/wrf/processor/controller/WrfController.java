package com.wrf.processor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/wrf")
public class WrfController {
    
    @Value("${wrf.python-script}")
    private String pythonScriptPath;
    
    @Value("${wrf.data-path}")
    private String dataPath;
    
    @Value("${wrf.timeout}")
    private long timeout;
    
    /**
     * 解析WRF文件
     * @param file WRF文件
     * @param height 高度
     * @return 解析结果
     */
    @PostMapping("/parse")
    public Map<String, Object> parseWrfFile(@RequestParam("file") MultipartFile file, @RequestParam(value = "height", defaultValue = "100") int height) {
        try {
            // 保存上传的文件
            Path uploadPath = Paths.get(dataPath, file.getOriginalFilename());
            Files.createDirectories(uploadPath.getParent());
            file.transferTo(uploadPath);
            
            // 执行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3", pythonScriptPath, uploadPath.toString(), String.valueOf(height)
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            
            // 解析结果
            // 这里需要添加JSON解析逻辑
            
            return Map.of(
                "success", true,
                "data", output
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
     * 获取处理后的气象数据
     * @param fileId 文件ID
     * @return 气象数据
     */
    @GetMapping("/data")
    public Map<String, Object> getWeatherData(@RequestParam("fileId") String fileId) {
        // 这里应该从数据库或缓存中获取数据
        return Map.of(
            "success", true,
            "data", Map.of()
        );
    }
    
    /**
     * 获取数据统计信息
     * @param fileId 文件ID
     * @return 统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getStatistics(@RequestParam("fileId") String fileId) {
        // 这里应该从数据库或缓存中获取统计信息
        return Map.of(
            "success", true,
            "data", Map.of()
        );
    }
}