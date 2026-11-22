package com.lifeos.document.infra;

import com.lifeos.document.domain.Document;
import com.lifeos.document.domain.DocumentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByHouseholdIdOrderByCreatedAtDesc(UUID householdId);
    List<Document> findByHouseholdIdAndExtractionStatusOrderByCreatedAtDesc(UUID householdId, DocumentStatus status);
    Optional<Document> findByIdAndHouseholdId(UUID id, UUID householdId);
}
