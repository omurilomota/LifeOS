package com.lifeos.notification.application;

import java.time.Instant;
import java.util.UUID;

public class NotificationDtos {
    private NotificationDtos() {}
    public record NotificationResponse(UUID id, String type, String title, String body, Instant readAt, String relatedEntityType, UUID relatedEntityId, Instant createdAt) {}
}
