package com.uav.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig 测试")
class SecurityConfigTest {

    @Mock
    private JwtFilter jwtFilter;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtFilter);
    }

    @Test
    @DisplayName("密码编码器BCrypt创建")
    void testPasswordEncoder() {
        assertNotNull(securityConfig.passwordEncoder());
    }

    @Test
    @DisplayName("认证管理器创建")
    void testAuthenticationManager() throws Exception {
        AuthenticationConfiguration authConfig = mock(AuthenticationConfiguration.class);
        when(authConfig.getAuthenticationManager()).thenReturn(null);
        
        assertNotNull(securityConfig.authenticationManager(authConfig));
    }
}

@DisplayName("SecurityAuditConfig 测试")
class SecurityAuditConfigTest {

    @Test
    @DisplayName("安全审计配置Bean创建")
    void testSecurityAuditConfigCreation() {
        SecurityAuditConfig config = new SecurityAuditConfig();
        assertNotNull(config);
        assertNotNull(config.csrfTokenRepository());
    }

    @Test
    @DisplayName("登出处理器创建")
    void testLogoutHandler() {
        SecurityAuditConfig config = new SecurityAuditConfig();
        assertNotNull(config.securityAuditLogoutHandler());
    }

    @Test
    @DisplayName("登出成功处理器创建")
    void testLogoutSuccessHandler() {
        SecurityAuditConfig config = new SecurityAuditConfig();
        assertNotNull(config.securityAuditLogoutSuccessHandler());
    }

    @Test
    @DisplayName("获取当前用户名-匿名")
    void testGetCurrentUsernameAnonymous() {
        SecurityAuditConfig config = new SecurityAuditConfig();
        String username = config.getCurrentUsername();
        assertNotNull(username);
    }

    @Test
    @DisplayName("记录用户活动")
    void testLogUserActivity() {
        SecurityAuditConfig config = new SecurityAuditConfig();
        assertDoesNotThrow(() -> config.logUserActivity("testuser", "测试", "详情"));
    }

    @Test
    @DisplayName("记录认证成功")
    void testLogAuthenticationSuccess() {
        SecurityAuditConfig config = new SecurityAuditConfig();
        assertDoesNotThrow(() -> config.logAuthenticationSuccess("testuser", null));
    }

    @Test
    @DisplayName("记录认证失败")
    void testLogAuthenticationFailure() {
        SecurityAuditConfig config = new SecurityAuditConfig();
        assertDoesNotThrow(() -> config.logAuthenticationFailure("testuser", "凭证错误", null));
    }
}