package com.lifeos.document.infra;

import com.lifeos.document.domain.ExtractionJob;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionJobRepository extends JpaRepository<ExtractionJob, UUID> {
    List<ExtractionJob> findByHouseholdIdAndDocumentIdOrderByCreatedAtDesc(UUID householdId, UUID documentId);
}
