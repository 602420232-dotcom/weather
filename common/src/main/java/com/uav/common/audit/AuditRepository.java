package com.uav.common.audit;

import java.util.List;

public interface AuditRepository {

    void save(AuditEntry entry);

    List<AuditEntry> findAll();

    List<AuditEntry> findByUsername(String username);

    List<AuditEntry> findByOperation(String operation);

    List<AuditEntry> findByStatus(String status);

    void clear();
}
