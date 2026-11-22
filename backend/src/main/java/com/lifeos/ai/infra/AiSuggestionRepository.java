package com.lifeos.ai.infra;

import com.lifeos.ai.domain.AiSuggestion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {
    List<AiSuggestion> findByHouseholdIdAndDocumentIdOrderByCreatedAtDesc(UUID householdId, UUID documentId);
    Optional<AiSuggestion> findByIdAndHouseholdId(UUID id, UUID householdId);
}
