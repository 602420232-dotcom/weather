package com.uav.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 数据库连接池配置
 * 优化HikariCP连接池参数以提升性能和稳定性
 */
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // 基础配置
        config.setJdbcUrl(datasourceUrl);
        config.setUsername(datasourceUsername);
        config.setPassword(datasourcePassword);
        config.setDriverClassName(driverClassName);
        
        // 连接池大小配置
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        
        // 连接超时配置
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        
        // 连接泄漏检测
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        
        // 连接验证
        config.setValidationTimeout(5000);
        config.setConnectionTestQuery("SELECT 1");
        
        // 连接池名称
        config.setPoolName("UAV-PathPlanning-Pool");
        
        // 队列等待
        config.setAllowPoolSuspension(false);
        config.setAutoCommit(true);
        
        return new HikariDataSource(config);
    }
}