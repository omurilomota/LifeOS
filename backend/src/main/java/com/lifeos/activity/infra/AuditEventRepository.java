package com.lifeos.activity.infra;

import com.lifeos.activity.domain.AuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByHouseholdIdAndEntityTypeAndEntityIdOrderByOccurredAtDesc(UUID householdId, String entityType, UUID entityId);
}
