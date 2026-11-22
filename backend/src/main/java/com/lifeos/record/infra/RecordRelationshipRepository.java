package com.lifeos.record.infra;

import com.lifeos.record.domain.RecordRelationship;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordRelationshipRepository extends JpaRepository<RecordRelationship, UUID> {
    List<RecordRelationship> findByHouseholdIdAndFromRecordId(UUID householdId, UUID fromRecordId);
    List<RecordRelationship> findByHouseholdIdAndToRecordId(UUID householdId, UUID toRecordId);
    boolean existsByHouseholdIdAndFromRecordIdAndToRecordIdAndRelationshipType(UUID householdId, UUID fromRecordId, UUID toRecordId, String relationshipType);
}
