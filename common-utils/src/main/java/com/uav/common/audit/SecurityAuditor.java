package com.uav.common.audit;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * @deprecated Use {@link SecurityAuditService} instead.
 * This class is kept for backward compatibility and delegates to the new service layer.
 * Static methods require Spring context to be available via {@link AuditContextHolder}.
 */
@Deprecated
public class SecurityAuditor {

    @Deprecated
    public static String getCurrentUsername() {
        return getService().getCurrentUsername();
    }

    @Deprecated
    public static String getClientIp(HttpServletRequest request) {
        return getService().getClientIp(request);
    }

    @Deprecated
    public static void logActivity(String username, String operation, String details) {
        getService().logActivity(username, operation, details);
    }

    @Deprecated
    public static void logWarning(String username, String operation, String warning) {
        getService().logWarning(username, operation, warning);
    }

    @Deprecated
    public static void logAuthenticationSuccess(String username, HttpServletRequest request) {
        getService().logAuthenticationSuccess(username, request);
    }

    @Deprecated
    public static void logAuthenticationFailure(String username, String reason, HttpServletRequest request) {
        getService().logAuthenticationFailure(username, reason, request);
    }

    @Deprecated
    public static void logAuthorizationDenied(String username, String resource, HttpServletRequest request) {
        getService().logAuthorizationDenied(username, resource, request);
    }

    @Deprecated
    public static List<AuditEntry> getAuditEntries() {
        return getService().getAuditEntries();
    }

    @Deprecated
    public static void clear() {
        getService().clear();
    }

    private static SecurityAuditService getService() {
        return AuditContextHolder.getSecurityAuditService();
    }
}
