package com.lifeos.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;
    @Column(name = "household_id", nullable = false)
    private UUID householdId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(nullable = false, length = 60)
    private String type;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String body;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "related_entity_type", length = 60)
    private String relatedEntityType;
    @Column(name = "related_entity_id")
    private UUID relatedEntityId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {}
    public Notification(UUID id, UUID householdId, UUID userId, String type, String title, String body, String relatedEntityType, UUID relatedEntityId, Instant createdAt) {
        this.id = id; this.householdId = householdId; this.userId = userId; this.type = type; this.title = title; this.body = body;
        this.relatedEntityType = relatedEntityType; this.relatedEntityId = relatedEntityId; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getHouseholdId() { return householdId; }
    public UUID getUserId() { return userId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
    public String getRelatedEntityType() { return relatedEntityType; }
    public UUID getRelatedEntityId() { return relatedEntityId; }
    public Instant getCreatedAt() { return createdAt; }
}
