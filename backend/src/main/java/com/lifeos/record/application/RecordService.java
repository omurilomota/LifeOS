package com.lifeos.record.application;

import com.lifeos.activity.application.AuditService;
import com.lifeos.activity.domain.AuditEvent;
import com.lifeos.activity.infra.AuditEventRepository;
import com.lifeos.household.domain.HouseholdMembership;
import com.lifeos.household.domain.HouseholdRole;
import com.lifeos.household.infra.HouseholdMembershipRepository;
import com.lifeos.record.domain.RecordEntity;
import com.lifeos.record.domain.RecordRelationship;
import com.lifeos.record.domain.RecordStatus;
import com.lifeos.record.domain.RecordType;
import com.lifeos.record.infra.RecordRelationshipRepository;
import com.lifeos.record.infra.RecordRepository;
import com.lifeos.shared.error.ApiException;
import com.lifeos.shared.time.TimeProvider;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordService {
    private static final String ENTITY_TYPE = "RECORD";

    private final RecordRepository recordRepository;
    private final RecordRelationshipRepository relationshipRepository;
    private final HouseholdMembershipRepository membershipRepository;
    private final AuditEventRepository auditEventRepository;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    public RecordService(RecordRepository recordRepository, RecordRelationshipRepository relationshipRepository,
                         HouseholdMembershipRepository membershipRepository, AuditEventRepository auditEventRepository,
                         AuditService auditService, TimeProvider timeProvider) {
        this.recordRepository = recordRepository;
        this.relationshipRepository = relationshipRepository;
        this.membershipRepository = membershipRepository;
        this.auditEventRepository = auditEventRepository;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    @Transactional(readOnly = true)
    public List<RecordDtos.RecordSummaryResponse> list(UUID userId, RecordStatus status, RecordType type, String q) {
        HouseholdMembership membership = requireMembership(userId);
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return recordRepository.search(membership.getHouseholdId(), status, type, query).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public RecordDtos.RecordResponse create(UUID userId, RecordDtos.RecordRequest request) {
        HouseholdMembership membership = requireWritableMembership(userId);
        validateResponsibleMember(membership.getHouseholdId(), request.responsibleMemberId());
        RecordStatus status = request.status() == null ? RecordStatus.ACTIVE : request.status();
        RecordEntity record = new RecordEntity(membership.getHouseholdId(), request.type(), request.title().trim(), status);
        apply(record, request, status);
        recordRepository.save(record);
        auditService.record(membership.getHouseholdId(), userId, ENTITY_TYPE, record.getId(), "RECORD_CREATED",
                Map.of("title", record.getTitle(), "type", record.getType().name()));
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public RecordDtos.RecordDetailResponse get(UUID userId, UUID recordId) {
        HouseholdMembership membership = requireMembership(userId);
        RecordEntity record = requireRecord(membership.getHouseholdId(), recordId);
        List<RecordDtos.RelationshipResponse> relationships = relationshipRepository.findByHouseholdIdAndFromRecordId(membership.getHouseholdId(), recordId)
                .stream().map(this::toRelationshipResponse).toList();
        return new RecordDtos.RecordDetailResponse(toResponse(record), relationships);
    }

    @Transactional
    public RecordDtos.RecordResponse update(UUID userId, UUID recordId, RecordDtos.RecordRequest request) {
        HouseholdMembership membership = requireWritableMembership(userId);
        RecordEntity record = requireRecord(membership.getHouseholdId(), recordId);
        validateResponsibleMember(membership.getHouseholdId(), request.responsibleMemberId());
        RecordStatus status = request.status() == null ? record.getStatus() : request.status();
        record.setType(request.type());
        record.setTitle(request.title().trim());
        apply(record, request, status);
        recordRepository.save(record);
        auditService.record(membership.getHouseholdId(), userId, ENTITY_TYPE, record.getId(), "RECORD_UPDATED",
                Map.of("title", record.getTitle(), "status", record.getStatus().name()));
        return toResponse(record);
    }

    @Transactional
    public RecordDtos.RecordResponse archive(UUID userId, UUID recordId) {
        HouseholdMembership membership = requireWritableMembership(userId);
        RecordEntity record = requireRecord(membership.getHouseholdId(), recordId);
        if (record.getStatus() != RecordStatus.ARCHIVED) {
            record.setStatus(RecordStatus.ARCHIVED);
            record.setArchivedAt(timeProvider.now());
            recordRepository.save(record);
            auditService.record(membership.getHouseholdId(), userId, ENTITY_TYPE, record.getId(), "RECORD_ARCHIVED", Map.of("title", record.getTitle()));
        }
        return toResponse(record);
    }

    @Transactional
    public RecordDtos.RelationshipResponse addRelationship(UUID userId, UUID recordId, RecordDtos.RelationshipRequest request) {
        HouseholdMembership membership = requireWritableMembership(userId);
        RecordEntity from = requireRecord(membership.getHouseholdId(), recordId);
        RecordEntity to = requireRecord(membership.getHouseholdId(), request.toRecordId());
        if (from.getId().equals(to.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RELATIONSHIP", "Record cannot be related to itself");
        }
        String relType = request.relationshipType().trim().toUpperCase();
        if (relationshipRepository.existsByHouseholdIdAndFromRecordIdAndToRecordIdAndRelationshipType(
                membership.getHouseholdId(), from.getId(), to.getId(), relType)) {
            throw new ApiException(HttpStatus.CONFLICT, "RELATIONSHIP_EXISTS", "Relationship already exists");
        }
        RecordRelationship relationship = new RecordRelationship(membership.getHouseholdId(), from.getId(), to.getId(), relType);
        relationshipRepository.save(relationship);
        auditService.record(membership.getHouseholdId(), userId, ENTITY_TYPE, from.getId(), "RECORD_RELATIONSHIP_ADDED",
                Map.of("toRecordId", to.getId().toString(), "relationshipType", relType));
        return toRelationshipResponse(relationship);
    }

    @Transactional(readOnly = true)
    public List<RecordDtos.AuditEventResponse> history(UUID userId, UUID recordId) {
        HouseholdMembership membership = requireMembership(userId);
        requireRecord(membership.getHouseholdId(), recordId);
        List<AuditEvent> events = auditEventRepository.findByHouseholdIdAndEntityTypeAndEntityIdOrderByOccurredAtDesc(
                membership.getHouseholdId(), ENTITY_TYPE, recordId);
        return events.stream().map(this::toAuditResponse).toList();
    }

    private void apply(RecordEntity record, RecordDtos.RecordRequest request, RecordStatus status) {
        record.setDescription(trimToNull(request.description()));
        record.setCategory(trimToNull(request.category()));
        record.setStatus(status);
        record.setResponsibleMemberId(request.responsibleMemberId());
        record.setAcquiredAt(request.acquiredAt());
        record.setStartedAt(request.startedAt());
        record.setAmountMinor(request.amountMinor());
        record.setCurrency(trimToNull(request.currency()));
        record.setMetadataJson(trimToNull(request.metadataJson()));
        if (status == RecordStatus.ARCHIVED) {
            if (record.getArchivedAt() == null) record.setArchivedAt(timeProvider.now());
        } else {
            record.setArchivedAt(null);
        }
    }

    private HouseholdMembership requireMembership(UUID userId) {
        return membershipRepository.findByUserId(userId).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "HOUSEHOLD_REQUIRED", "Household membership required"));
    }

    private HouseholdMembership requireWritableMembership(UUID userId) {
        HouseholdMembership membership = requireMembership(userId);
        if (membership.getRole() == HouseholdRole.VIEWER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Viewer role cannot modify records");
        }
        return membership;
    }

    private RecordEntity requireRecord(UUID householdId, UUID recordId) {
        return recordRepository.findByIdAndHouseholdId(recordId, householdId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RECORD_NOT_FOUND", "Record not found"));
    }

    private void validateResponsibleMember(UUID householdId, UUID responsibleMemberId) {
        if (responsibleMemberId == null) return;
        membershipRepository.findById(responsibleMemberId)
                .filter(m -> m.getHouseholdId().equals(householdId))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RESPONSIBLE_MEMBER", "Responsible member is invalid"));
    }

    private RecordDtos.RecordResponse toResponse(RecordEntity r) {
        return new RecordDtos.RecordResponse(r.getId(), r.getHouseholdId(), r.getType(), r.getTitle(), r.getDescription(), r.getCategory(),
                r.getStatus(), r.getResponsibleMemberId(), r.getAcquiredAt(), r.getStartedAt(), r.getArchivedAt(),
                r.getAmountMinor(), r.getCurrency(), r.getMetadataJson(), r.getCreatedAt(), r.getUpdatedAt());
    }

    private RecordDtos.RecordSummaryResponse toSummary(RecordEntity r) {
        return new RecordDtos.RecordSummaryResponse(r.getId(), r.getType(), r.getTitle(), r.getCategory(), r.getStatus(), r.getResponsibleMemberId(), r.getUpdatedAt());
    }

    private RecordDtos.RelationshipResponse toRelationshipResponse(RecordRelationship r) {
        return new RecordDtos.RelationshipResponse(r.getId(), r.getFromRecordId(), r.getToRecordId(), r.getRelationshipType(), r.getCreatedAt());
    }

    private RecordDtos.AuditEventResponse toAuditResponse(AuditEvent e) {
        return new RecordDtos.AuditEventResponse(e.getId(), e.getActorUserId(), e.getEntityType(), e.getEntityId(), e.getAction(), e.getMetadataJson(), e.getOccurredAt());
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
