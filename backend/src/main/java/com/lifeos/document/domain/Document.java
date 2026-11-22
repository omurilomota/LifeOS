package com.lifeos.document.domain;

import com.lifeos.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document extends BaseEntity {

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private UUID uploadedByUserId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", nullable = false, length = 30)
    private DocumentStatus extractionStatus;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "extraction_error", length = 500)
    private String extractionError;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private DocumentReviewStatus reviewStatus = DocumentReviewStatus.PENDING;

    protected Document() {}

    public Document(UUID householdId, UUID uploadedByUserId, String originalFilename, String storageKey,
                    String mimeType, long sizeBytes, String checksumSha256, DocumentStatus extractionStatus) {
        this.householdId = householdId;
        this.uploadedByUserId = uploadedByUserId;
        this.originalFilename = originalFilename;
        this.storageKey = storageKey;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.checksumSha256 = checksumSha256;
        this.extractionStatus = extractionStatus;
    }

    public UUID getHouseholdId() { return householdId; }
    public UUID getUploadedByUserId() { return uploadedByUserId; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStorageKey() { return storageKey; }
    public String getMimeType() { return mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getChecksumSha256() { return checksumSha256; }
    public DocumentStatus getExtractionStatus() { return extractionStatus; }
    public void setExtractionStatus(DocumentStatus extractionStatus) { this.extractionStatus = extractionStatus; }
    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
    public String getExtractionError() { return extractionError; }
    public void setExtractionError(String extractionError) { this.extractionError = extractionError; }
    public DocumentReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(DocumentReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
}
