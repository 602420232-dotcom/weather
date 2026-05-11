package com.uav.config;

import com.uav.common.audit.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditConfig {

    private final SecurityAuditService securityAuditService;

    public SecurityAuditConfig(SecurityAuditService securityAuditService) {
        this.securityAuditService = securityAuditService;
    }

    public String getCurrentUsername() {
        return securityAuditService.getCurrentUsername();
    }

    public void logUserActivity(String username, String operation, String details) {
        securityAuditService.logActivity(username, operation, details);
    }

    public void logSecurityWarning(String username, String operation, String warning) {
        securityAuditService.logWarning(username, operation, warning);
    }

    public void logAuthenticationSuccess(String username, HttpServletRequest request) {
        securityAuditService.logAuthenticationSuccess(username, request);
    }

    public void logAuthenticationFailure(String username, String reason, HttpServletRequest request) {
        securityAuditService.logAuthenticationFailure(username, reason, request);
    }

    public void logAuthorizationDenied(String username, String resource, HttpServletRequest request) {
        securityAuditService.logAuthorizationDenied(username, resource, request);
    }
}
