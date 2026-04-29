package com.uav.controller;

import com.uav.config.SecurityAuditConfig;
import com.uav.service.DataSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/data-sources")
public class DataSourceController {
    
    @Autowired
    private DataSourceService dataSourceService;
    
    @GetMapping
    public ResponseEntity<?> getDataSourceList(HttpServletRequest request) {
        try {
            String username = SecurityAuditConfig.getCurrentUsername();
            List<Map<String, Object>> dataSources = dataSourceService.listDataSources();
            
            // 记录操作
            SecurityAuditConfig.logUserActivity(username, "获取数据源列表", "成功获取数据源列表");
            
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取数据源列表成功",
                "data", dataSources
            ));
        } catch (Exception e) {
            String username = SecurityAuditConfig.getCurrentUsername();
            SecurityAuditConfig.logUserActivity(username, "获取数据源列表", "失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", "获取数据源列表失败",
                "details", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getDataSourceById(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = SecurityAuditConfig.getCurrentUsername();
            Map<String, Object> dataSource = dataSourceService.getDataSourceById(id);
            
            if (dataSource == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", 404,
                    "message", "数据源不存在",
                    "details", null
                ));
            }
            
            // 记录操作
            SecurityAuditConfig.logUserActivity(username, "获取数据源详情", "成功获取数据源ID: " + id);
            
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取数据源详情成功",
                "data", dataSource
            ));
        } catch (Exception e) {
            String username = SecurityAuditConfig.getCurrentUsername();
            SecurityAuditConfig.logUserActivity(username, "获取数据源详情", "失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", "获取数据源详情失败",
                "details", e.getMessage()
            ));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createDataSource(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            String username = SecurityAuditConfig.getCurrentUsername();
            Map<String, Object> dataSource = dataSourceService.createDataSource(requestBody);
            
            // 记录操作
            SecurityAuditConfig.logUserActivity(username, "创建数据源", "成功创建数据源: " + requestBody.get("name"));
            
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "创建数据源成功",
                "data", dataSource
            ));
        } catch (Exception e) {
            String username = SecurityAuditConfig.getCurrentUsername();
            SecurityAuditConfig.logUserActivity(username, "创建数据源", "失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", "创建数据源失败",
                "details", e.getMessage()
            ));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDataSource(@PathVariable Long id, @RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            String username = SecurityAuditConfig.getCurrentUsername();
            Map<String, Object> dataSource = dataSourceService.updateDataSource(id, requestBody);
            
            if (dataSource == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", 404,
                    "message", "数据源不存在",
                    "details", null
                ));
            }
            
            // 记录操作
            SecurityAuditConfig.logUserActivity(username, "更新数据源", "成功更新数据源ID: " + id);
            
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "更新数据源成功",
                "data", dataSource
            ));
        } catch (Exception e) {
            String username = SecurityAuditConfig.getCurrentUsername();
            SecurityAuditConfig.logUserActivity(username, "更新数据源", "失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", "更新数据源失败",
                "details", e.getMessage()
            ));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDataSource(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = SecurityAuditConfig.getCurrentUsername();
            boolean success = dataSourceService.deleteDataSource(id);
            
            if (!success) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", 404,
                    "message", "数据源不存在",
                    "details", null
                ));
            }
            
            // 记录操作
            SecurityAuditConfig.logUserActivity(username, "删除数据源", "成功删除数据源ID: " + id);
            
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "删除数据源成功",
                "data", null
            ));
        } catch (Exception e) {
            String username = SecurityAuditConfig.getCurrentUsername();
            SecurityAuditConfig.logUserActivity(username, "删除数据源", "失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", "删除数据源失败",
                "details", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/test")
    public ResponseEntity<?> testDataSource(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            String username = SecurityAuditConfig.getCurrentUsername();
            Map<String, Object> result = dataSourceService.testDataSource(requestBody);
            
            // 记录操作
            SecurityAuditConfig.logUserActivity(username, "测试数据源", "成功测试数据源: " + requestBody.get("type"));
            
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "测试数据源成功",
                "data", result
            ));
        } catch (Exception e) {
            String username = SecurityAuditConfig.getCurrentUsername();
            SecurityAuditConfig.logUserActivity(username, "测试数据源", "失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", "测试数据源失败",
                "details", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/types")
    public ResponseEntity<?> getDataSourceTypes(HttpServletRequest request) {
        try {
            String username = SecurityAuditConfig.getCurrentUsername();
            List<Map<String, Object>> types = dataSourceService.getDataSourceTypes();
            
            // 记录操作
            SecurityAuditConfig.logUserActivity(username, "获取数据源类型", "成功获取数据源类型列表");
            
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取数据源类型成功",
                "data", types
            ));
        } catch (Exception e) {
            String username = SecurityAuditConfig.getCurrentUsername();
            SecurityAuditConfig.logUserActivity(username, "获取数据源类型", "失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", "获取数据源类型失败",
                "details", e.getMessage()
            ));
        }
    }
}