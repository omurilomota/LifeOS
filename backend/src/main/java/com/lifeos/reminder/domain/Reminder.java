package com.lifeos.reminder.domain;

import com.lifeos.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reminders")
public class Reminder extends BaseEntity {
    @Column(name = "household_id", nullable = false)
    private UUID householdId;
    @Column(name = "obligation_id", nullable = false)
    private UUID obligationId;
    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;
    @Column(nullable = false, length = 30)
    private String channel;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    @Column(name = "dedupe_key", nullable = false, length = 200)
    private String dedupeKey;

    protected Reminder() {}
    public Reminder(UUID householdId, UUID obligationId, Instant scheduledAt, String channel, String status, String dedupeKey) {
        this.householdId = householdId; this.obligationId = obligationId; this.scheduledAt = scheduledAt;
        this.channel = channel; this.status = status; this.dedupeKey = dedupeKey;
    }
    public UUID getHouseholdId() { return householdId; }
    public UUID getObligationId() { return obligationId; }
    public Instant getScheduledAt() { return scheduledAt; }
    public String getChannel() { return channel; }
    public String getStatus() { return status; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public String getDedupeKey() { return dedupeKey; }
    public void setStatus(String status) { this.status = status; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
}
