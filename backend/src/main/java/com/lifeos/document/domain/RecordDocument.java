package com.lifeos.document.domain;

import com.lifeos.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "record_documents")
public class RecordDocument extends BaseEntity {

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "link_type", nullable = false, length = 60)
    private String linkType;

    protected RecordDocument() {}

    public RecordDocument(UUID householdId, UUID recordId, UUID documentId, String linkType) {
        this.householdId = householdId;
        this.recordId = recordId;
        this.documentId = documentId;
        this.linkType = linkType;
    }

    public UUID getHouseholdId() { return householdId; }
    public UUID getRecordId() { return recordId; }
    public UUID getDocumentId() { return documentId; }
    public String getLinkType() { return linkType; }
}
