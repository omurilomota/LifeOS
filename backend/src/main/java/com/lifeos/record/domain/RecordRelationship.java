package com.lifeos.record.domain;

import com.lifeos.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "record_relationships")
public class RecordRelationship extends BaseEntity {

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "from_record_id", nullable = false)
    private UUID fromRecordId;

    @Column(name = "to_record_id", nullable = false)
    private UUID toRecordId;

    @Column(name = "relationship_type", nullable = false, length = 60)
    private String relationshipType;

    protected RecordRelationship() {}

    public RecordRelationship(UUID householdId, UUID fromRecordId, UUID toRecordId, String relationshipType) {
        this.householdId = householdId;
        this.fromRecordId = fromRecordId;
        this.toRecordId = toRecordId;
        this.relationshipType = relationshipType;
    }

    public UUID getHouseholdId() { return householdId; }
    public UUID getFromRecordId() { return fromRecordId; }
    public UUID getToRecordId() { return toRecordId; }
    public String getRelationshipType() { return relationshipType; }
}
