package com.lifeos.identity.application;

import com.lifeos.activity.infra.AuditEventRepository;
import com.lifeos.document.infra.DocumentRepository;
import com.lifeos.document.infra.RecordDocumentRepository;
import com.lifeos.household.domain.HouseholdMembership;
import com.lifeos.household.infra.HouseholdInvitationRepository;
import com.lifeos.household.infra.HouseholdMembershipRepository;
import com.lifeos.household.infra.HouseholdRepository;
import com.lifeos.identity.infra.UserRepository;
import com.lifeos.notification.infra.NotificationRepository;
import com.lifeos.obligation.infra.ObligationRepository;
import com.lifeos.record.infra.RecordRepository;
import com.lifeos.reminder.infra.ReminderRepository;
import com.lifeos.shared.error.ApiException;
import com.lifeos.shared.storage.StorageService;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataPrivacyService {
    private static final Logger log = LoggerFactory.getLogger(DataPrivacyService.class);

    private final UserRepository userRepository;
    private final HouseholdMembershipRepository membershipRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdInvitationRepository invitationRepository;
    private final RecordRepository recordRepository;
    private final DocumentRepository documentRepository;
    private final RecordDocumentRepository recordDocumentRepository;
    private final ObligationRepository obligationRepository;
    private final ReminderRepository reminderRepository;
    private final NotificationRepository notificationRepository;
    private final AuditEventRepository auditEventRepository;
    private final StorageService storageService;

    public DataPrivacyService(UserRepository userRepository, HouseholdMembershipRepository membershipRepository,
                              HouseholdRepository householdRepository, HouseholdInvitationRepository invitationRepository,
                              RecordRepository recordRepository, DocumentRepository documentRepository,
                              RecordDocumentRepository recordDocumentRepository, ObligationRepository obligationRepository,
                              ReminderRepository reminderRepository, NotificationRepository notificationRepository,
                              AuditEventRepository auditEventRepository, StorageService storageService) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.householdRepository = householdRepository;
        this.invitationRepository = invitationRepository;
        this.recordRepository = recordRepository;
        this.documentRepository = documentRepository;
        this.recordDocumentRepository = recordDocumentRepository;
        this.obligationRepository = obligationRepository;
        this.reminderRepository = reminderRepository;
        this.notificationRepository = notificationRepository;
        this.auditEventRepository = auditEventRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportData(UUID userId) {
        HouseholdMembership m = requireMembership(userId);
        UUID hid = m.getHouseholdId();
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("user", userRepository.findById(userId).map(u -> Map.of("id", u.getId(), "email", u.getEmail(), "displayName", u.getDisplayName(), "locale", u.getLocale(), "timezone", u.getTimezone())).orElse(null));
        export.put("household", householdRepository.findById(hid).map(h -> Map.of("id", h.getId(), "name", h.getName())).orElse(null));
        export.put("memberships", membershipRepository.findByHouseholdId(hid).stream().map(mm -> Map.of("id", mm.getId(), "userId", mm.getUserId(), "role", mm.getRole().name())).toList());
        export.put("records", recordRepository.search(hid, null, null, null).stream().map(r -> Map.of("id", r.getId(), "title", r.getTitle(), "type", r.getType().name(), "status", r.getStatus().name())).toList());
        export.put("documents", documentRepository.findByHouseholdIdOrderByCreatedAtDesc(hid).stream().map(d -> Map.of("id", d.getId(), "filename", d.getOriginalFilename(), "mimeType", d.getMimeType(), "sizeBytes", d.getSizeBytes())).toList());
        export.put("obligations", obligationRepository.list(hid, null).stream().map(o -> Map.of("id", o.getId(), "title", o.getTitle(), "status", o.getStatus().name(), "dueAt", o.getDueAt().toString())).toList());
        export.put("notifications", notificationRepository.findByUserIdAndHouseholdIdOrderByCreatedAtDesc(userId, hid).stream().map(n -> Map.of("id", n.getId(), "type", n.getType(), "title", n.getTitle(), "createdAt", n.getCreatedAt().toString())).toList());
        return export;
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        HouseholdMembership m = requireMembership(userId);
        UUID hid = m.getHouseholdId();
        long memberCount = membershipRepository.findByHouseholdId(hid).size();

        notificationRepository.findByUserIdAndHouseholdIdOrderByCreatedAtDesc(userId, hid).forEach(notificationRepository::delete);
        membershipRepository.findByHouseholdIdAndUserId(hid, userId).ifPresent(membershipRepository::delete);

        // If last member, delete household data (simple LGPD baseline)
        if (memberCount <= 1) {
            documentRepository.findByHouseholdIdOrderByCreatedAtDesc(hid).forEach(d -> {
                try { storageService.delete(d.getStorageKey()); } catch (IOException e) { log.warn("Failed to delete storage object for document {}", d.getId()); }
                recordDocumentRepository.deleteByHouseholdIdAndDocumentId(hid, d.getId());
            });
            documentRepository.findByHouseholdIdOrderByCreatedAtDesc(hid).forEach(documentRepository::delete);
            obligationRepository.list(hid, null).forEach(obligationRepository::delete);
            recordRepository.search(hid, null, null, null).forEach(recordRepository::delete);
            invitationRepository.findByHouseholdIdOrderByCreatedAtDesc(hid).forEach(invitationRepository::delete);
            householdRepository.findById(hid).ifPresent(householdRepository::delete);
        }
        userRepository.findById(userId).ifPresent(userRepository::delete);
    }

    private HouseholdMembership requireMembership(UUID userId) {
        return membershipRepository.findByUserId(userId).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HOUSEHOLD_NOT_FOUND", "Household not found"));
    }
}
