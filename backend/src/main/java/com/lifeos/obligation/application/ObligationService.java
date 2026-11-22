package com.lifeos.obligation.application;

import com.lifeos.activity.application.AuditService;
import com.lifeos.document.infra.DocumentRepository;
import com.lifeos.household.domain.HouseholdMembership;
import com.lifeos.household.domain.HouseholdRole;
import com.lifeos.household.infra.HouseholdMembershipRepository;
import com.lifeos.notification.domain.Notification;
import com.lifeos.notification.infra.NotificationRepository;
import com.lifeos.obligation.domain.Obligation;
import com.lifeos.obligation.domain.ObligationPriority;
import com.lifeos.obligation.domain.ObligationStatus;
import com.lifeos.obligation.infra.ObligationRepository;
import com.lifeos.record.infra.RecordRepository;
import com.lifeos.reminder.domain.Reminder;
import com.lifeos.reminder.infra.ReminderRepository;
import com.lifeos.shared.error.ApiException;
import com.lifeos.shared.time.TimeProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObligationService {
    private static final String ENTITY = "OBLIGATION";

    private final ObligationRepository obligationRepository;
    private final HouseholdMembershipRepository membershipRepository;
    private final RecordRepository recordRepository;
    private final DocumentRepository documentRepository;
    private final ReminderRepository reminderRepository;
    private final NotificationRepository notificationRepository;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    public ObligationService(ObligationRepository obligationRepository, HouseholdMembershipRepository membershipRepository,
                             RecordRepository recordRepository, DocumentRepository documentRepository,
                             ReminderRepository reminderRepository, NotificationRepository notificationRepository,
                             AuditService auditService, TimeProvider timeProvider) {
        this.obligationRepository = obligationRepository;
        this.membershipRepository = membershipRepository;
        this.recordRepository = recordRepository;
        this.documentRepository = documentRepository;
        this.reminderRepository = reminderRepository;
        this.notificationRepository = notificationRepository;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    @Transactional(readOnly = true)
    public List<ObligationDtos.ObligationResponse> list(UUID userId, ObligationStatus status) {
        HouseholdMembership m = requireMembership(userId);
        return obligationRepository.list(m.getHouseholdId(), status).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ObligationDtos.ObligationResponse create(UUID userId, ObligationDtos.ObligationRequest request) {
        HouseholdMembership m = requireWritable(userId);
        validateRefs(m.getHouseholdId(), request.recordId(), request.responsibleMemberId(), null);
        ObligationStatus status = request.status() == null ? ObligationStatus.OPEN : request.status();
        Obligation o = new Obligation(m.getHouseholdId(), request.title().trim(), request.dueAt(),
                request.priority() == null ? ObligationPriority.MEDIUM : request.priority(), status);
        apply(o, request, status);
        obligationRepository.save(o);
        createReminderAndNotification(m, o, userId);
        auditService.record(m.getHouseholdId(), userId, ENTITY, o.getId(), "OBLIGATION_CREATED", Map.of("title", o.getTitle()));
        return toResponse(o);
    }

    @Transactional(readOnly = true)
    public ObligationDtos.ObligationResponse get(UUID userId, UUID id) {
        HouseholdMembership m = requireMembership(userId);
        return toResponse(requireObligation(m.getHouseholdId(), id));
    }

    @Transactional
    public ObligationDtos.ObligationResponse update(UUID userId, UUID id, ObligationDtos.ObligationRequest request) {
        HouseholdMembership m = requireWritable(userId);
        Obligation o = requireObligation(m.getHouseholdId(), id);
        validateRefs(m.getHouseholdId(), request.recordId(), request.responsibleMemberId(), null);
        ObligationStatus status = request.status() == null ? o.getStatus() : request.status();
        o.setTitle(request.title().trim());
        o.setDueAt(request.dueAt());
        o.setPriority(request.priority() == null ? o.getPriority() : request.priority());
        apply(o, request, status);
        obligationRepository.save(o);
        syncReminder(o);
        auditService.record(m.getHouseholdId(), userId, ENTITY, o.getId(), "OBLIGATION_UPDATED", Map.of("status", o.getStatus().name()));
        return toResponse(o);
    }

    @Transactional
    public ObligationDtos.ObligationResponse complete(UUID userId, UUID id) {
        HouseholdMembership m = requireWritable(userId);
        Obligation o = requireObligation(m.getHouseholdId(), id);
        if (o.getStatus() != ObligationStatus.COMPLETED) {
            o.setStatus(ObligationStatus.COMPLETED);
            o.setCompletedAt(timeProvider.now());
            o.setCancelledAt(null);
            obligationRepository.save(o);
            auditService.record(m.getHouseholdId(), userId, ENTITY, o.getId(), "OBLIGATION_COMPLETED", Map.of("title", o.getTitle()));
            createNextRecurrenceIfNeeded(m, o, userId);
        }
        return toResponse(o);
    }

    @Transactional
    public ObligationDtos.ObligationResponse cancel(UUID userId, UUID id) {
        HouseholdMembership m = requireWritable(userId);
        Obligation o = requireObligation(m.getHouseholdId(), id);
        if (o.getStatus() != ObligationStatus.CANCELLED) {
            o.setStatus(ObligationStatus.CANCELLED);
            o.setCancelledAt(timeProvider.now());
            o.setCompletedAt(null);
            obligationRepository.save(o);
            auditService.record(m.getHouseholdId(), userId, ENTITY, o.getId(), "OBLIGATION_CANCELLED", Map.of("title", o.getTitle()));
        }
        return toResponse(o);
    }

    @Transactional
    public ObligationDtos.ObligationResponse reschedule(UUID userId, UUID id, Instant dueAt) {
        HouseholdMembership m = requireWritable(userId);
        Obligation o = requireObligation(m.getHouseholdId(), id);
        if (o.getStatus() != ObligationStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS", "Only open obligations can be rescheduled");
        }
        o.setDueAt(dueAt);
        obligationRepository.save(o);
        syncReminder(o);
        auditService.record(m.getHouseholdId(), userId, ENTITY, o.getId(), "OBLIGATION_RESCHEDULED", Map.of("dueAt", dueAt.toString()));
        return toResponse(o);
    }

    private void apply(Obligation o, ObligationDtos.ObligationRequest r, ObligationStatus status) {
        o.setDescription(trimToNull(r.description()));
        o.setRecordId(r.recordId());
        o.setResponsibleMemberId(r.responsibleMemberId());
        o.setRecurrenceRule(normalizeRecurrence(r.recurrenceRule()));
        o.setStatus(status);
        Instant now = timeProvider.now();
        if (status == ObligationStatus.COMPLETED) { if (o.getCompletedAt() == null) o.setCompletedAt(now); o.setCancelledAt(null); }
        else if (status == ObligationStatus.CANCELLED) { if (o.getCancelledAt() == null) o.setCancelledAt(now); o.setCompletedAt(null); }
        else { o.setCompletedAt(null); o.setCancelledAt(null); }
    }

    private void createReminderAndNotification(HouseholdMembership m, Obligation o, UUID actorUserId) {
        syncReminder(o);
        notificationRepository.save(new Notification(UUID.randomUUID(), m.getHouseholdId(), actorUserId, "OBLIGATION_DUE",
                "Obrigação criada", o.getTitle(), ENTITY, o.getId(), timeProvider.now()));
    }

    private void syncReminder(Obligation o) {
        Instant scheduledAt = o.getDueAt().isBefore(timeProvider.now()) ? timeProvider.now() : o.getDueAt();
        String dedupe = "obl:" + o.getId() + ":due:" + o.getDueAt();
        if (!reminderRepository.existsByDedupeKey(dedupe)) {
            reminderRepository.save(new Reminder(o.getHouseholdId(), o.getId(), scheduledAt, "IN_APP", "PENDING", dedupe));
        }
    }

    private void createNextRecurrenceIfNeeded(HouseholdMembership m, Obligation completed, UUID actorUserId) {
        Instant nextDue = nextRecurrence(completed.getRecurrenceRule(), completed.getDueAt());
        if (nextDue == null) return;
        Obligation next = new Obligation(m.getHouseholdId(), completed.getTitle(), nextDue, completed.getPriority(), ObligationStatus.OPEN);
        next.setDescription(completed.getDescription());
        next.setRecordId(completed.getRecordId());
        next.setResponsibleMemberId(completed.getResponsibleMemberId());
        next.setRecurrenceRule(completed.getRecurrenceRule());
        next.setSourceType("RECURRENCE");
        next.setSourceDocumentId(completed.getSourceDocumentId());
        obligationRepository.save(next);
        syncReminder(next);
        auditService.record(m.getHouseholdId(), actorUserId, ENTITY, next.getId(), "OBLIGATION_RECURRENCE_CREATED", Map.of("fromObligationId", completed.getId().toString()));
    }

    private Instant nextRecurrence(String rule, Instant dueAt) {
        if (rule == null || rule.isBlank()) return null;
        return switch (rule.trim().toUpperCase()) {
            case "DAILY" -> dueAt.plus(1, ChronoUnit.DAYS);
            case "WEEKLY" -> dueAt.plus(7, ChronoUnit.DAYS);
            case "MONTHLY" -> dueAt.plus(30, ChronoUnit.DAYS);
            case "YEARLY" -> dueAt.plus(365, ChronoUnit.DAYS);
            default -> null;
        };
    }

    private String normalizeRecurrence(String rule) {
        String v = trimToNull(rule);
        if (v == null) return null;
        String upper = v.toUpperCase();
        return switch (upper) { case "DAILY", "WEEKLY", "MONTHLY", "YEARLY" -> upper; default -> throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RECURRENCE", "Recurrence rule must be DAILY, WEEKLY, MONTHLY, or YEARLY"); };
    }

    private void validateRefs(UUID householdId, UUID recordId, UUID responsibleMemberId, UUID sourceDocumentId) {
        if (recordId != null) recordRepository.findByIdAndHouseholdId(recordId, householdId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RECORD", "Record is invalid"));
        if (responsibleMemberId != null) membershipRepository.findById(responsibleMemberId)
                .filter(mm -> mm.getHouseholdId().equals(householdId))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RESPONSIBLE_MEMBER", "Responsible member is invalid"));
        if (sourceDocumentId != null) documentRepository.findByIdAndHouseholdId(sourceDocumentId, householdId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT", "Document is invalid"));
    }

    private HouseholdMembership requireMembership(UUID userId) {
        return membershipRepository.findByUserId(userId).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "HOUSEHOLD_REQUIRED", "Household membership required"));
    }
    private HouseholdMembership requireWritable(UUID userId) {
        HouseholdMembership m = requireMembership(userId);
        if (m.getRole() == HouseholdRole.VIEWER) throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Viewer role cannot modify obligations");
        return m;
    }
    private Obligation requireObligation(UUID householdId, UUID id) {
        return obligationRepository.findByIdAndHouseholdId(id, householdId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "OBLIGATION_NOT_FOUND", "Obligation not found"));
    }
    private ObligationDtos.ObligationResponse toResponse(Obligation o) {
        return new ObligationDtos.ObligationResponse(o.getId(), o.getHouseholdId(), o.getRecordId(), o.getTitle(), o.getDescription(),
                o.getResponsibleMemberId(), o.getDueAt(), o.getPriority(), o.getStatus(), o.getRecurrenceRule(), o.getSourceType(),
                o.getSourceDocumentId(), o.getCompletedAt(), o.getCancelledAt(), o.getCreatedAt(), o.getUpdatedAt());
    }
    private String trimToNull(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
}
