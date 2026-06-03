package com.uav.wrf.processor.service;

import com.uav.wrf.processor.entity.WrfDataFile;
import com.uav.wrf.processor.repository.WrfDataFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WrfDataService {

    private final WrfDataFileRepository wrfDataFileRepository;

    public Optional<WrfDataFile> findByFileId(String fileId) {
        return wrfDataFileRepository.findByFileId(fileId);
    }

    public Optional<WrfDataFile> findById(Long id) {
        return wrfDataFileRepository.findById(Objects.requireNonNull(id));
    }

    public Page<WrfDataFile> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return wrfDataFileRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public WrfDataFile createWrfDataFile(String fileName, String filePath, Long fileSize) {
        String fileId = "wrf_" + UUID.randomUUID().toString().substring(0, 8);
        WrfDataFile wrfDataFile = WrfDataFile.builder()
                .fileId(fileId)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(fileSize)
                .status("UPLOADED")
                .height(100)
                .timeSteps(24)
                .variables("temperature,humidity,wind_speed,wind_direction,pressure,turbulence,visibility,lightning_risk")
                .build();
        return wrfDataFileRepository.save(Objects.requireNonNull(wrfDataFile));
    }

    public WrfDataFile updateWrfDataFile(WrfDataFile wrfDataFile) {
        return wrfDataFileRepository.save(Objects.requireNonNull(wrfDataFile));
    }

    public Map<String, Object> getWeatherData(String fileId) {
        Optional<WrfDataFile> optionalWrfDataFile = findByFileId(fileId);
        if (optionalWrfDataFile.isEmpty()) {
            return Map.of("success", false, "message", "File not found: " + fileId);
        }
        WrfDataFile wrfDataFile = optionalWrfDataFile.get();
        Map<String, Object> weatherData = new HashMap<>();
        weatherData.put("fileId", fileId);
        weatherData.put("height", wrfDataFile.getHeight());
        weatherData.put("timeSteps", wrfDataFile.getTimeSteps());
        weatherData.put("variables", wrfDataFile.getVariables() != null
            ? Arrays.asList(wrfDataFile.getVariables().split(","))
            : List.of());
        weatherData.put("wind_speed", generateGridData(10, 10, 5.0, 15.0));
        weatherData.put("wind_direction", generateGridData(10, 10, 0.0, 360.0));
        weatherData.put("temperature", generateGridData(10, 10, 15.0, 30.0));
        weatherData.put("humidity", generateGridData(10, 10, 40.0, 80.0));
        weatherData.put("pressure", generateGridData(10, 10, 1000.0, 1020.0));
        return Map.of("success", true, "data", weatherData);
    }

    public Map<String, Object> getTurbulence(String fileId) {
        Optional<WrfDataFile> optional = findByFileId(fileId);
        if (optional.isEmpty()) {
            return Map.of("success", false, "message", "File not found: " + fileId);
        }
        WrfDataFile file = optional.get();
        Map<String, Object> turbulence = new HashMap<>();
        turbulence.put("fileId", fileId);
        turbulence.put("tke_mean", 1.2);
        turbulence.put("tke_max", 2.8);
        turbulence.put("tke_min", 0.3);
        turbulence.put("turbulence_intensity", "MODERATE");
        turbulence.put("turbulence_description", "中等湍流 - 需谨慎飞行");
        turbulence.put("dissipation_rate", 0.13);
        turbulence.put("height", file.getHeight());
        turbulence.put("timestamp", System.currentTimeMillis());
        return Map.of("success", true, "data", turbulence);
    }

    public Map<String, Object> getVisibility(String fileId) {
        Optional<WrfDataFile> optional = findByFileId(fileId);
        if (optional.isEmpty()) {
            return Map.of("success", false, "message", "File not found: " + fileId);
        }
        Map<String, Object> visibility = new HashMap<>();
        visibility.put("fileId", fileId);
        visibility.put("visibility_m", 8500.0);
        visibility.put("visibility_category", "GOOD");
        visibility.put("visibility_description", "能见度良好，可正常飞行");
        visibility.put("extinction_coefficient", 0.46);
        visibility.put("temperature_c", 22.5);
        visibility.put("humidity_percent", 65.0);
        visibility.put("timestamp", System.currentTimeMillis());
        return Map.of("success", true, "data", visibility);
    }

    public Map<String, Object> getLightningRisk(String fileId) {
        Optional<WrfDataFile> optional = findByFileId(fileId);
        if (optional.isEmpty()) {
            return Map.of("success", false, "message", "File not found: " + fileId);
        }
        Map<String, Object> risk = new HashMap<>();
        risk.put("fileId", fileId);
        risk.put("risk_level", "LOW");
        risk.put("risk_score", 28.5);
        risk.put("risk_description", "闪电风险低 - 可正常飞行");
        risk.put("cape", 680.0);
        risk.put("cin", -35.0);
        risk.put("lcl", 850.0);
        risk.put("timestamp", System.currentTimeMillis());
        return Map.of("success", true, "data", risk);
    }

    public Map<String, Object> getHeightLayers(String fileId, List<Integer> layers) {
        Optional<WrfDataFile> optional = findByFileId(fileId);
        if (optional.isEmpty()) {
            return Map.of("success", false, "message", "File not found: " + fileId);
        }
        List<Map<String, Object>> layerDataList = new ArrayList<>();
        for (int h : layers) {
            Map<String, Object> layer = new HashMap<>();
            layer.put("height", h);
            double heightFactor = 1 + 0.3 * Math.log((double) h / 100 + 1);
            if (h == 0) heightFactor = 0.1;
            layer.put("wind_speed", Math.round(8.5 * heightFactor * 100.0) / 100.0);
            layer.put("wind_direction", Math.round((180 + Math.random() * 90) * 10.0) / 10.0);
            layer.put("temperature", Math.round((25.0 - 0.0065 * h) * 10.0) / 10.0);
            layer.put("humidity", Math.round(Math.max(10, 65 - 0.002 * h) * 10.0) / 10.0);
            double tke = Math.max(0, 1.2 * Math.exp(-(double) h / 500));
            layer.put("turbulence_tke", Math.round(tke * 10000.0) / 10000.0);
            if (tke >= 3.0) layer.put("turbulence_intensity", "SEVERE");
            else if (tke >= 1.5) layer.put("turbulence_intensity", "MODERATE");
            else if (tke >= 0.5) layer.put("turbulence_intensity", "LOW");
            else layer.put("turbulence_intensity", "NEGLIGIBLE");
            layerDataList.add(layer);
        }
        return Map.of(
            "success", true,
            "data", Map.of("fileId", fileId, "layers", layerDataList, "layer_count", layers.size(), "timestamp", System.currentTimeMillis())
        );
    }

    public Map<String, Object> getStatistics(String fileId) {
        Optional<WrfDataFile> optionalWrfDataFile = findByFileId(fileId);
        if (optionalWrfDataFile.isEmpty()) {
            return Map.of("success", false, "message", "File not found: " + fileId);
        }
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("fileId", fileId);
        statistics.put("wind_speed", Map.of("min", 5.2, "max", 14.8, "mean", 9.5, "std", 2.3));
        statistics.put("temperature", Map.of("min", 15.1, "max", 29.8, "mean", 22.3, "std", 3.1));
        statistics.put("humidity", Map.of("min", 41.2, "max", 79.5, "mean", 60.8, "std", 8.2));
        statistics.put("turbulence_tke", Map.of("min", 0.3, "max", 2.8, "mean", 1.2, "std", 0.6));
        statistics.put("visibility_m", Map.of("min", 5000, "max", 20000, "mean", 12000, "std", 3500));
        statistics.put("lightning_risk_score", Map.of("min", 5, "max", 75, "mean", 28, "std", 15));
        return Map.of("success", true, "data", statistics);
    }

    public Map<String, Object> getDetail(Long id) {
        Optional<WrfDataFile> optionalWrfDataFile = findById(id);
        if (optionalWrfDataFile.isEmpty()) {
            return Map.of("success", false, "message", "Record not found with id: " + id);
        }
        WrfDataFile wrfDataFile = optionalWrfDataFile.get();
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", wrfDataFile.getId());
        detail.put("fileId", wrfDataFile.getFileId());
        detail.put("fileName", wrfDataFile.getFileName());
        detail.put("fileSize", wrfDataFile.getFileSize());
        detail.put("height", wrfDataFile.getHeight());
        detail.put("timeSteps", wrfDataFile.getTimeSteps());
        detail.put("variables", wrfDataFile.getVariables() != null
            ? Arrays.asList(wrfDataFile.getVariables().split(","))
            : List.of());
        detail.put("status", wrfDataFile.getStatus());
        detail.put("createdAt", wrfDataFile.getCreatedAt() != null ? wrfDataFile.getCreatedAt().toString() : null);
        detail.put("updatedAt", wrfDataFile.getUpdatedAt() != null ? wrfDataFile.getUpdatedAt().toString() : null);
        return Map.of("success", true, "data", detail);
    }

    private double[][] generateGridData(int rows, int cols, double min, double max) {
        double[][] grid = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = min + Math.random() * (max - min);
            }
        }
        return grid;
    }
}
