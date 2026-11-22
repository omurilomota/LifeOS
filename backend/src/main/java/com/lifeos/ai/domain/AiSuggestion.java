package com.lifeos.ai.domain;

import com.lifeos.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_suggestions")
public class AiSuggestion extends BaseEntity {
    @Column(name = "household_id", nullable = false)
    private UUID householdId;
    @Column(name = "document_id", nullable = false)
    private UUID documentId;
    @Column(nullable = false, length = 60)
    private String provider;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "suggestion_type", nullable = false, length = 40)
    private String suggestionType;
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;
    @Column(length = 20)
    private String confidence;
    @Column(name = "evidence_text", columnDefinition = "TEXT")
    private String evidenceText;
    @Column(name = "created_by_user_id")
    private UUID createdByUserId;
    @Column(name = "confirmed_by_user_id")
    private UUID confirmedByUserId;
    @Column(name = "confirmed_at")
    private Instant confirmedAt;
    @Column(name = "rejected_at")
    private Instant rejectedAt;

    protected AiSuggestion() {}

    public AiSuggestion(UUID householdId, UUID documentId, String provider, String status, String suggestionType,
                        String payloadJson, String confidence, String evidenceText, UUID createdByUserId) {
        this.householdId = householdId; this.documentId = documentId; this.provider = provider; this.status = status;
        this.suggestionType = suggestionType; this.payloadJson = payloadJson; this.confidence = confidence;
        this.evidenceText = evidenceText; this.createdByUserId = createdByUserId;
    }

    public UUID getHouseholdId() { return householdId; }
    public UUID getDocumentId() { return documentId; }
    public String getProvider() { return provider; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSuggestionType() { return suggestionType; }
    public String getPayloadJson() { return payloadJson; }
    public String getConfidence() { return confidence; }
    public String getEvidenceText() { return evidenceText; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public UUID getConfirmedByUserId() { return confirmedByUserId; }
    public void setConfirmedByUserId(UUID confirmedByUserId) { this.confirmedByUserId = confirmedByUserId; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
    public Instant getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }
}
