package com.lifeos.activity.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.activity.domain.AuditEvent;
import com.lifeos.activity.infra.AuditEventRepository;
import com.lifeos.shared.time.TimeProvider;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditEventRepository auditEventRepository;
    private final TimeProvider timeProvider;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository auditEventRepository, TimeProvider timeProvider, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.timeProvider = timeProvider;
        this.objectMapper = objectMapper;
    }

    public void record(UUID householdId, UUID actorUserId, String entityType, UUID entityId, String action, Map<String, Object> metadata) {
        String metadataJson = null;
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize audit metadata for entityType={} entityId={}", entityType, entityId);
            }
        }
        AuditEvent event = new AuditEvent(UUID.randomUUID(), householdId, actorUserId, entityType, entityId, action, metadataJson, timeProvider.now());
        auditEventRepository.save(event);
    }
}
