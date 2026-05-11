package com.uav.common.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class InMemoryAuditRepository implements AuditRepository {

    private final ConcurrentLinkedQueue<AuditEntry> entries = new ConcurrentLinkedQueue<>();
    private static final int MAX_ENTRIES = 10000;

    @Override
    public void save(AuditEntry entry) {
        entries.offer(entry);
        log.debug("Audit entry saved: user={}, operation={}, status={}", 
                entry.getUsername(), entry.getOperation(), entry.getStatus());
        while (entries.size() > MAX_ENTRIES) {
            entries.poll();
        }
    }

    @Override
    public List<AuditEntry> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    @Override
    public List<AuditEntry> findByUsername(String username) {
        return entries.stream()
                .filter(e -> username.equals(e.getUsername()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditEntry> findByOperation(String operation) {
        return entries.stream()
                .filter(e -> operation.equals(e.getOperation()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditEntry> findByStatus(String status) {
        return entries.stream()
                .filter(e -> status.equals(e.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public void clear() {
        int size = entries.size();
        entries.clear();
        log.info("Audit entries cleared: removed {} entries", size);
    }
}
