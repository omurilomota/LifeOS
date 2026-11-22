package com.lifeos.record.domain;

import com.lifeos.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "records")
public class RecordEntity extends BaseEntity {

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecordType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 80)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecordStatus status;

    @Column(name = "responsible_member_id")
    private UUID responsibleMemberId;

    @Column(name = "acquired_at")
    private LocalDate acquiredAt;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "amount_minor")
    private Long amountMinor;

    @Column(length = 3)
    private String currency;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    protected RecordEntity() {}

    public RecordEntity(UUID householdId, RecordType type, String title, RecordStatus status) {
        this.householdId = householdId;
        this.type = type;
        this.title = title;
        this.status = status;
    }

    public UUID getHouseholdId() { return householdId; }
    public RecordType getType() { return type; }
    public void setType(RecordType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public RecordStatus getStatus() { return status; }
    public void setStatus(RecordStatus status) { this.status = status; }
    public UUID getResponsibleMemberId() { return responsibleMemberId; }
    public void setResponsibleMemberId(UUID responsibleMemberId) { this.responsibleMemberId = responsibleMemberId; }
    public LocalDate getAcquiredAt() { return acquiredAt; }
    public void setAcquiredAt(LocalDate acquiredAt) { this.acquiredAt = acquiredAt; }
    public LocalDate getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDate startedAt) { this.startedAt = startedAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public Long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(Long amountMinor) { this.amountMinor = amountMinor; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
