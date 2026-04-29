package com.uav.controller;

import com.uav.config.SecurityAuditConfig;
import com.uav.service.RealDataSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/real-data")
public class RealDataSourceController {
    
    @Autowired
    private RealDataSourceService realDataSourceService;
    
    @GetMapping("/ground-station")
    public ResponseEntity<?> getGroundStationData(HttpServletRequest request) {
        try {
            String username = SecurityAuditConfig.getCurrentUsername();
            List<Map<String, Object>> data = realDataSourceService.getGroundStationData();
            
            // 记录操作
            SecurityAuditConfig.logUserActivity(username, "获取地面站数据", "成功获取地面站数据");
            
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取地面站数据成功",
                "data", data
            ));
        } catch (Exception e) {
            String username = SecurityAuditConfig.getCurrentUsername();
            SecurityAuditConfig.logUserActivity(username, "获取地面站数据", "失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", "获取地面站数据失败",
                "details", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/buoy")
    public ResponseEntity<?> getBuoyData(HttpServletRequest request) {
        try {
            String username = SecurityAuditConfig.getCurrentUsername();
            List<Map<String, Object>> data = realDataSourceService.getBuoyData();
            
            // 记录操作
            SecurityAuditConfig.logUserActivity(username, "获取浮标数据", "成功获取浮标数据");
            
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取浮标数据成功",
                "data", data
            ));
        } catch (Exception e) {
            String username = SecurityAuditConfig.getCurrentUsername();
            SecurityAuditConfig.logUserActivity(username, "获取浮标数据", "失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", "获取浮标数据失败",
                "details", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> getDataSourceStatus(HttpServletRequest request) {
        try {
            String username = SecurityAuditConfig.getCurrentUsername();
            Map<String, Object> status = realDataSourceService.getDataSourceStatus();
            
            // 记录操作
            SecurityAuditConfig.logUserActivity(username, "获取数据源状态", "成功获取数据源状态");
            
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取数据源状态成功",
                "data", status
            ));
        } catch (Exception e) {
            String username = SecurityAuditConfig.getCurrentUsername();
            SecurityAuditConfig.logUserActivity(username, "获取数据源状态", "失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", "获取数据源状态失败",
                "details", e.getMessage()
            ));
        }
    }
}