package com.lifeos.ai.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AiSuggestionDtos {
    private AiSuggestionDtos() {}

    public record SuggestionResponse(UUID id, UUID documentId, String provider, String status, String suggestionType,
                                    Map<String, Object> payload, String confidence, String evidenceText, Instant createdAt) {}
    public record ConfirmResponse(UUID suggestionId, String status, UUID createdEntityId, String createdEntityType) {}
}
