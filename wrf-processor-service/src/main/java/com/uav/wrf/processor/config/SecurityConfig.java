package com.uav.wrf.processor.config;

import com.uav.common.config.CommonSecurityConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * WRF处理器服务安全配置
 * 
 * 通过 @Import 导入通用安全配置，使用统一的认证和授权策略
 */
@Configuration
@EnableWebSecurity
@Import(CommonSecurityConfig.class)
public class SecurityConfig {
}
