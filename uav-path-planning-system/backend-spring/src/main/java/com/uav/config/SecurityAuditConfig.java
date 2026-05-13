package com.uav.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@EnableMethodSecurity
public class SecurityAuditConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityAuditConfig.class);

    @Bean
    public LogoutHandler securityAuditLogoutHandler() {
        return (request, response, authentication) -> {
            if (authentication != null) {
                log.debug("用户登出: {}", authentication.getName());
            }
        };
    }

    @Bean
    public LogoutSuccessHandler securityAuditLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            if (authentication != null) {
                log.debug("用户登出成功: {}", authentication.getName());
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
        log.debug("用户认证成功: username={}", username);
    }

    public void logAuthenticationFailure(String username, String error, HttpServletRequest request) {
        log.warn("用户认证失败: username={}", username);
    }

    public void logAuthorizationDenied(String username, String resource, HttpServletRequest request) {
        log.warn("访问被拒绝: username={}, resource={}", username, resource);
    }

    public void logUserActivity(String username, String activity, String details) {
        log.debug("用户活动: username={}, operation={}", username, activity);
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}
