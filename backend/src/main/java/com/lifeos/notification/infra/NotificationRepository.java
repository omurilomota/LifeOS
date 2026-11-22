package com.lifeos.notification.infra;

import com.lifeos.notification.domain.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdAndHouseholdIdOrderByCreatedAtDesc(UUID userId, UUID householdId);
    Optional<Notification> findByIdAndUserIdAndHouseholdId(UUID id, UUID userId, UUID householdId);
    boolean existsByUserIdAndRelatedEntityTypeAndRelatedEntityIdAndType(UUID userId, String relatedEntityType, UUID relatedEntityId, String type);
}
