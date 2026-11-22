package com.lifeos.document.infra;

import com.lifeos.document.domain.RecordDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordDocumentRepository extends JpaRepository<RecordDocument, UUID> {
    List<RecordDocument> findByHouseholdIdAndRecordId(UUID householdId, UUID recordId);
    List<RecordDocument> findByHouseholdIdAndDocumentId(UUID householdId, UUID documentId);
    boolean existsByRecordIdAndDocumentIdAndLinkType(UUID recordId, UUID documentId, String linkType);
    void deleteByHouseholdIdAndDocumentId(UUID householdId, UUID documentId);
}
