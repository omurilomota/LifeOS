package com.lifeos.ai.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.activity.application.AuditService;
import com.lifeos.ai.domain.AiSuggestion;
import com.lifeos.ai.infra.AiSuggestionRepository;
import com.lifeos.document.domain.Document;
import com.lifeos.document.infra.DocumentRepository;
import com.lifeos.household.domain.HouseholdMembership;
import com.lifeos.household.domain.HouseholdRole;
import com.lifeos.household.infra.HouseholdMembershipRepository;
import com.lifeos.obligation.application.ObligationDtos;
import com.lifeos.obligation.application.ObligationService;
import com.lifeos.obligation.domain.ObligationPriority;
import com.lifeos.obligation.domain.ObligationStatus;
import com.lifeos.record.application.RecordDtos;
import com.lifeos.record.application.RecordService;
import com.lifeos.record.domain.RecordStatus;
import com.lifeos.record.domain.RecordType;
import com.lifeos.shared.error.ApiException;
import com.lifeos.shared.time.TimeProvider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableConfigurationProperties(AiProperties.class)
public class AiSuggestionService {
    private static final String ENTITY = "AI_SUGGESTION";

    private final AiProperties properties;
    private final AiSuggestionRepository suggestionRepository;
    private final DocumentRepository documentRepository;
    private final HouseholdMembershipRepository membershipRepository;
    private final RecordService recordService;
    private final ObligationService obligationService;
    private final AuditService auditService;
    private final TimeProvider timeProvider;
    private final ObjectMapper objectMapper;

    public AiSuggestionService(AiProperties properties, AiSuggestionRepository suggestionRepository,
                               DocumentRepository documentRepository, HouseholdMembershipRepository membershipRepository,
                               RecordService recordService, ObligationService obligationService,
                               AuditService auditService, TimeProvider timeProvider, ObjectMapper objectMapper) {
        this.properties = properties;
        this.suggestionRepository = suggestionRepository;
        this.documentRepository = documentRepository;
        this.membershipRepository = membershipRepository;
        this.recordService = recordService;
        this.obligationService = obligationService;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public boolean isEnabled() { return properties.enabled(); }

    @Transactional
    public List<AiSuggestionDtos.SuggestionResponse> generateForDocument(UUID userId, UUID documentId) {
        HouseholdMembership m = requireWritable(userId);
        if (!properties.enabled()) {
            throw new ApiException(HttpStatus.PRECONDITION_FAILED, "AI_DISABLED", "AI is disabled. Manual flow remains available.");
        }
        Document doc = documentRepository.findByIdAndHouseholdId(documentId, m.getHouseholdId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document not found"));

        String name = doc.getOriginalFilename();
        String baseTitle = name.replaceFirst("\\.[^.]+$", "").replace('_', ' ').trim();
        if (baseTitle.isBlank()) baseTitle = "Documento";

        Map<String, Object> recordPayload = Map.of(
                "type", "OTHER",
                "title", baseTitle,
                "description", "Sugestão gerada a partir do documento. Revise antes de confirmar.",
                "status", "ACTIVE"
        );
        Map<String, Object> obligationPayload = Map.of(
                "title", "Revisar documento: " + baseTitle,
                "description", "Obrigação sugerida por IA. Confirme ou edite antes de criar.",
                "priority", "MEDIUM",
                "status", "OPEN",
                "dueAt", timeProvider.now().plusSeconds(7 * 24 * 3600).toString()
        );

        AiSuggestion rec = saveSuggestion(m, doc, "RECORD", recordPayload, "LOW", "Nome do arquivo e metadados do documento");
        AiSuggestion obl = saveSuggestion(m, doc, "OBLIGATION", obligationPayload, "LOW", "Sugestão padrão baseada no upload; sem data inventada pelo provedor externo");
        auditService.record(m.getHouseholdId(), userId, ENTITY, rec.getId(), "AI_SUGGESTION_CREATED", Map.of("documentId", documentId.toString(), "type", "RECORD"));
        auditService.record(m.getHouseholdId(), userId, ENTITY, obl.getId(), "AI_SUGGESTION_CREATED", Map.of("documentId", documentId.toString(), "type", "OBLIGATION"));
        return List.of(toResponse(rec), toResponse(obl));
    }

    @Transactional(readOnly = true)
    public List<AiSuggestionDtos.SuggestionResponse> listForDocument(UUID userId, UUID documentId) {
        HouseholdMembership m = requireMembership(userId);
        documentRepository.findByIdAndHouseholdId(documentId, m.getHouseholdId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document not found"));
        return suggestionRepository.findByHouseholdIdAndDocumentIdOrderByCreatedAtDesc(m.getHouseholdId(), documentId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AiSuggestionDtos.ConfirmResponse confirm(UUID userId, UUID suggestionId) {
        HouseholdMembership m = requireWritable(userId);
        AiSuggestion s = suggestionRepository.findByIdAndHouseholdId(suggestionId, m.getHouseholdId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUGGESTION_NOT_FOUND", "Suggestion not found"));
        if (!"PENDING".equals(s.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SUGGESTION_NOT_PENDING", "Suggestion is not pending");
        }
        Map<String, Object> payload = readPayload(s.getPayloadJson());
        UUID createdId;
        String entityType;
        if ("RECORD".equals(s.getSuggestionType())) {
            RecordDtos.RecordRequest req = new RecordDtos.RecordRequest(
                    RecordType.valueOf(string(payload, "type", "OTHER")),
                    string(payload, "title", "Documento"),
                    string(payload, "description", null),
                    string(payload, "category", null),
                    RecordStatus.valueOf(string(payload, "status", "ACTIVE")),
                    null, null, null, null, null, null
            );
            createdId = recordService.create(userId, req).id();
            entityType = "RECORD";
        } else if ("OBLIGATION".equals(s.getSuggestionType())) {
            ObligationDtos.ObligationRequest req = new ObligationDtos.ObligationRequest(
                    string(payload, "title", "Revisar documento"),
                    string(payload, "description", null),
                    null, null,
                    Instant.parse(string(payload, "dueAt", timeProvider.now().plusSeconds(86400).toString())),
                    ObligationPriority.valueOf(string(payload, "priority", "MEDIUM")),
                    ObligationStatus.valueOf(string(payload, "status", "OPEN")),
                    null
            );
            createdId = obligationService.create(userId, req).id();
            entityType = "OBLIGATION";
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SUGGESTION_TYPE", "Invalid suggestion type");
        }
        s.setStatus("CONFIRMED");
        s.setConfirmedByUserId(userId);
        s.setConfirmedAt(timeProvider.now());
        suggestionRepository.save(s);
        auditService.record(m.getHouseholdId(), userId, ENTITY, s.getId(), "AI_SUGGESTION_CONFIRMED", Map.of("createdEntityType", entityType, "createdEntityId", createdId.toString()));
        return new AiSuggestionDtos.ConfirmResponse(s.getId(), s.getStatus(), createdId, entityType);
    }

    @Transactional
    public AiSuggestionDtos.SuggestionResponse reject(UUID userId, UUID suggestionId) {
        HouseholdMembership m = requireWritable(userId);
        AiSuggestion s = suggestionRepository.findByIdAndHouseholdId(suggestionId, m.getHouseholdId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUGGESTION_NOT_FOUND", "Suggestion not found"));
        if ("PENDING".equals(s.getStatus())) {
            s.setStatus("REJECTED");
            s.setRejectedAt(timeProvider.now());
            suggestionRepository.save(s);
            auditService.record(m.getHouseholdId(), userId, ENTITY, s.getId(), "AI_SUGGESTION_REJECTED", Map.of("documentId", s.getDocumentId().toString()));
        }
        return toResponse(s);
    }

    private AiSuggestion saveSuggestion(HouseholdMembership m, Document doc, String type, Map<String, Object> payload, String confidence, String evidence) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            AiSuggestion s = new AiSuggestion(m.getHouseholdId(), doc.getId(), properties.provider(), "PENDING", type, json, confidence, evidence, m.getUserId());
            return suggestionRepository.save(s);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "AI_SUGGESTION_ERROR", "Failed to create suggestion");
        }
    }

    private AiSuggestionDtos.SuggestionResponse toResponse(AiSuggestion s) {
        return new AiSuggestionDtos.SuggestionResponse(s.getId(), s.getDocumentId(), s.getProvider(), s.getStatus(), s.getSuggestionType(), readPayload(s.getPayloadJson()), s.getConfidence(), s.getEvidenceText(), s.getCreatedAt());
    }

    private Map<String, Object> readPayload(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return Map.of(); }
    }

    private String string(Map<String, Object> payload, String key, String def) {
        Object v = payload.get(key);
        return v == null ? def : String.valueOf(v);
    }

    private HouseholdMembership requireMembership(UUID userId) {
        return membershipRepository.findByUserId(userId).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "HOUSEHOLD_REQUIRED", "Household membership required"));
    }

    private HouseholdMembership requireWritable(UUID userId) {
        HouseholdMembership m = requireMembership(userId);
        if (m.getRole() == HouseholdRole.VIEWER) throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Viewer role cannot manage AI suggestions");
        return m;
    }
}
