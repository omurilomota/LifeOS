package com.lifeos.obligation.application;

import com.lifeos.obligation.domain.ObligationPriority;
import com.lifeos.obligation.domain.ObligationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public class ObligationDtos {
    private ObligationDtos() {}

    public record ObligationRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            UUID recordId,
            UUID responsibleMemberId,
            @NotNull Instant dueAt,
            @NotNull ObligationPriority priority,
            ObligationStatus status,
            @Size(max = 120) String recurrenceRule
    ) {}

    public record RescheduleRequest(@NotNull Instant dueAt) {}

    public record ObligationResponse(
            UUID id,
            UUID householdId,
            UUID recordId,
            String title,
            String description,
            UUID responsibleMemberId,
            Instant dueAt,
            ObligationPriority priority,
            ObligationStatus status,
            String recurrenceRule,
            String sourceType,
            UUID sourceDocumentId,
            Instant completedAt,
            Instant cancelledAt,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
