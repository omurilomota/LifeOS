package com.lifeos.document.application;

import com.lifeos.document.domain.DocumentReviewStatus;
import com.lifeos.document.domain.DocumentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public class DocumentDtos {
    private DocumentDtos() {}

    public record DocumentResponse(
            UUID id,
            UUID householdId,
            UUID uploadedByUserId,
            String originalFilename,
            String mimeType,
            long sizeBytes,
            String checksumSha256,
            DocumentStatus extractionStatus,
            String extractedText,
            String extractionError,
            DocumentReviewStatus reviewStatus,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record LinkDocumentRequest(
            @NotNull UUID recordId,
            @NotBlank @Size(max = 60) String linkType
    ) {}

    public record DocumentLinkResponse(
            UUID id,
            UUID recordId,
            UUID documentId,
            String linkType,
            Instant createdAt
    ) {}
}
