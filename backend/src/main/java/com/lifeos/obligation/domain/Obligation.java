package com.lifeos.obligation.domain;

import com.lifeos.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "obligations")
public class Obligation extends BaseEntity {

    @Column(name = "household_id", nullable = false)
    private UUID householdId;
    @Column(name = "record_id")
    private UUID recordId;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "responsible_member_id")
    private UUID responsibleMemberId;
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ObligationPriority priority;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ObligationStatus status;
    @Column(name = "recurrence_rule", length = 120)
    private String recurrenceRule;
    @Column(name = "source_type", nullable = false, length = 40)
    private String sourceType = "MANUAL";
    @Column(name = "source_document_id")
    private UUID sourceDocumentId;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Obligation() {}

    public Obligation(UUID householdId, String title, Instant dueAt, ObligationPriority priority, ObligationStatus status) {
        this.householdId = householdId;
        this.title = title;
        this.dueAt = dueAt;
        this.priority = priority;
        this.status = status;
    }

    public UUID getHouseholdId() { return householdId; }
    public UUID getRecordId() { return recordId; }
    public void setRecordId(UUID recordId) { this.recordId = recordId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getResponsibleMemberId() { return responsibleMemberId; }
    public void setResponsibleMemberId(UUID responsibleMemberId) { this.responsibleMemberId = responsibleMemberId; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public ObligationPriority getPriority() { return priority; }
    public void setPriority(ObligationPriority priority) { this.priority = priority; }
    public ObligationStatus getStatus() { return status; }
    public void setStatus(ObligationStatus status) { this.status = status; }
    public String getRecurrenceRule() { return recurrenceRule; }
    public void setRecurrenceRule(String recurrenceRule) { this.recurrenceRule = recurrenceRule; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public UUID getSourceDocumentId() { return sourceDocumentId; }
    public void setSourceDocumentId(UUID sourceDocumentId) { this.sourceDocumentId = sourceDocumentId; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
}
