package com.uav.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class SecurityAuditService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditRepository auditRepository;
    private final IpAddressExtractor ipAddressExtractor;
    private final CurrentUserService currentUserService;

    public SecurityAuditService(AuditRepository auditRepository,
                                IpAddressExtractor ipAddressExtractor,
                                CurrentUserService currentUserService) {
        this.auditRepository = auditRepository;
        this.ipAddressExtractor = ipAddressExtractor;
        this.currentUserService = currentUserService;
    }

    public String getCurrentUsername() {
        return currentUserService.getCurrentUsername();
    }

    public String getClientIp(HttpServletRequest request) {
        return ipAddressExtractor.extractClientIp(request);
    }

    public void logActivity(String username, String operation, String details) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        AuditEntry entry = new AuditEntry(timestamp, username, operation, details, "success");
        auditRepository.save(entry);
        log.info("[AUDIT] {} | {} | {} | {}", timestamp, username, operation, details);
    }

    public void logWarning(String username, String operation, String warning) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        AuditEntry entry = new AuditEntry(timestamp, username, operation, warning, "warning");
        auditRepository.save(entry);
        log.warn("[WARNING] {} | {} | {} | {}", timestamp, username, operation, warning);
    }

    public void logAuthenticationSuccess(String username, HttpServletRequest request) {
        String ip = getClientIp(request);
        String timestamp = LocalDateTime.now().format(FORMATTER);
        AuditEntry entry = new AuditEntry(timestamp, username, "LOGIN_SUCCESS", "ip=" + ip, "success");
        auditRepository.save(entry);
        log.info("[AUTH] {} | {} logged in from {}", timestamp, username, ip);
    }

    public void logAuthenticationFailure(String username, String reason, HttpServletRequest request) {
        String ip = getClientIp(request);
        String timestamp = LocalDateTime.now().format(FORMATTER);
        AuditEntry entry = new AuditEntry(timestamp, username, "LOGIN_FAILURE",
                "ip=" + ip + ", reason=" + reason, "failure");
        auditRepository.save(entry);
        log.warn("[AUTH] {} | {} failed login from {}: {}", timestamp, username, ip, reason);
    }

    public void logAuthorizationDenied(String username, String resource, HttpServletRequest request) {
        String ip = getClientIp(request);
        String timestamp = LocalDateTime.now().format(FORMATTER);
        AuditEntry entry = new AuditEntry(timestamp, username, "ACCESS_DENIED",
                "resource=" + resource + ", ip=" + ip, "denied");
        auditRepository.save(entry);
        log.warn("[AUTH] {} | {} denied access to {} from {}", timestamp, username, resource, ip);
    }

    public List<AuditEntry> getAuditEntries() {
        return auditRepository.findAll();
    }

    public void clear() {
        auditRepository.clear();
    }
}
