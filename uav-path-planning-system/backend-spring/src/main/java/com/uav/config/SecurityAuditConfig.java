package com.uav.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
@Slf4j
@EnableMethodSecurity
public class SecurityAuditConfig {

    @Bean
    public LogoutHandler securityAuditLogoutHandler() {
        return (request, response, authentication) -> {
            if (authentication != null) {
                String username = authentication.getName();
                log.info("用户登出: " + username + "，IP: " + request.getRemoteAddr());
            }
        };
    }

    @Bean
    public LogoutSuccessHandler securityAuditLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            if (authentication != null) {
                String username = authentication.getName();
                log.info("用户登出成功: " + username);
            }
            response.sendRedirect("/login");
        };
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-XSRF-TOKEN");
        return repository;
    }

    public void logAuthenticationSuccess(String username, HttpServletRequest request) {
        log.info("用户登录成功: " + username + "，IP: " + request.getRemoteAddr());
    }

    public void logAuthenticationFailure(String username, String error, HttpServletRequest request) {
        log.warn("用户登录失败: " + username + "，错误: " + error + "，IP: " + request.getRemoteAddr());
    }

    public void logAuthorizationDenied(String username, String resource, HttpServletRequest request) {
        log.warn("用户访问被拒绝: " + username + "，资源: " + resource + "，IP: " + request.getRemoteAddr());
    }

    public void logUserActivity(String username, String activity, String details) {
        log.info("用户活动: " + username + "，操作: " + activity + "，详情: " + details);
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}
