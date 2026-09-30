package com.example.banking.service;

import com.example.banking.entity.AuditLog;
import com.example.banking.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Async("auditExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String actor, String action, String entityType, String entityId,
                       Object beforeState, Object afterState, String ipAddress) {
        try {
            AuditLog audit = AuditLog.builder()
                .actor(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .beforeState(toJson(beforeState))
                .afterState(toJson(afterState))
                .ipAddress(ipAddress)
                .build();
            auditLogRepository.save(audit);
        } catch (Exception ex) {
            log.error("Failed to record audit log: {}", ex.getMessage(), ex);
        }
    }

    private String toJson(Object o) {
        if (o == null) return null;
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{\"error\":\"serialization failed\"}";
        }
    }
}