package com.lifeos.record.application;

import com.lifeos.record.domain.RecordStatus;
import com.lifeos.record.domain.RecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class RecordDtos {
    private RecordDtos() {}

    public record RecordRequest(
            @NotNull RecordType type,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @Size(max = 80) String category,
            RecordStatus status,
            UUID responsibleMemberId,
            LocalDate acquiredAt,
            LocalDate startedAt,
            Long amountMinor,
            @Size(min = 3, max = 3) String currency,
            String metadataJson
    ) {}

    public record RecordResponse(
            UUID id,
            UUID householdId,
            RecordType type,
            String title,
            String description,
            String category,
            RecordStatus status,
            UUID responsibleMemberId,
            LocalDate acquiredAt,
            LocalDate startedAt,
            Instant archivedAt,
            Long amountMinor,
            String currency,
            String metadataJson,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record RecordSummaryResponse(
            UUID id,
            RecordType type,
            String title,
            String category,
            RecordStatus status,
            UUID responsibleMemberId,
            Instant updatedAt
    ) {}

    public record RelationshipRequest(
            @NotNull UUID toRecordId,
            @NotBlank @Size(max = 60) String relationshipType
    ) {}

    public record RelationshipResponse(
            UUID id,
            UUID fromRecordId,
            UUID toRecordId,
            String relationshipType,
            Instant createdAt
    ) {}

    public record RecordDetailResponse(
            RecordResponse record,
            List<RelationshipResponse> relationships
    ) {}

    public record AuditEventResponse(
            UUID id,
            UUID actorUserId,
            String entityType,
            UUID entityId,
            String action,
            String metadataJson,
            Instant occurredAt
    ) {}
}
