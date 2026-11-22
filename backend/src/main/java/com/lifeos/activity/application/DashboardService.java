package com.lifeos.activity.application;

import com.lifeos.household.domain.HouseholdMembership;
import com.lifeos.household.infra.HouseholdMembershipRepository;
import com.lifeos.notification.application.NotificationDtos;
import com.lifeos.notification.infra.NotificationRepository;
import com.lifeos.obligation.application.ObligationDtos;
import com.lifeos.obligation.domain.ObligationStatus;
import com.lifeos.obligation.infra.ObligationRepository;
import com.lifeos.record.application.RecordDtos;
import com.lifeos.record.infra.RecordRepository;
import com.lifeos.shared.error.ApiException;
import com.lifeos.shared.time.TimeProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final HouseholdMembershipRepository membershipRepository;
    private final ObligationRepository obligationRepository;
    private final RecordRepository recordRepository;
    private final NotificationRepository notificationRepository;
    private final TimeProvider timeProvider;

    public DashboardService(HouseholdMembershipRepository membershipRepository, ObligationRepository obligationRepository,
                            RecordRepository recordRepository, NotificationRepository notificationRepository, TimeProvider timeProvider) {
        this.membershipRepository = membershipRepository;
        this.obligationRepository = obligationRepository;
        this.recordRepository = recordRepository;
        this.notificationRepository = notificationRepository;
        this.timeProvider = timeProvider;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.DashboardResponse get(UUID userId) {
        HouseholdMembership m = membershipRepository.findByUserId(userId).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "HOUSEHOLD_REQUIRED", "Household membership required"));
        UUID hid = m.getHouseholdId();
        Instant now = timeProvider.now();
        Instant soon = now.plus(7, ChronoUnit.DAYS);

        List<ObligationDtos.ObligationResponse> overdue = obligationRepository.findByHouseholdIdAndStatusAndDueAtBeforeOrderByDueAtAsc(hid, ObligationStatus.OPEN, now)
                .stream().limit(10).map(this::toObligation).toList();
        List<ObligationDtos.ObligationResponse> dueSoon = obligationRepository.findByHouseholdIdAndStatusAndDueAtBetweenOrderByDueAtAsc(hid, ObligationStatus.OPEN, now, soon)
                .stream().limit(10).map(this::toObligation).toList();
        List<RecordDtos.RecordSummaryResponse> recentRecords = recordRepository.search(hid, null, null, null).stream().limit(5)
                .map(r -> new RecordDtos.RecordSummaryResponse(r.getId(), r.getType(), r.getTitle(), r.getCategory(), r.getStatus(), r.getResponsibleMemberId(), r.getUpdatedAt())).toList();
        List<NotificationDtos.NotificationResponse> notifications = notificationRepository.findByUserIdAndHouseholdIdOrderByCreatedAtDesc(userId, hid).stream().limit(10)
                .map(n -> new NotificationDtos.NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getReadAt(), n.getRelatedEntityType(), n.getRelatedEntityId(), n.getCreatedAt())).toList();

        return new DashboardDtos.DashboardResponse(overdue, dueSoon, recentRecords, notifications);
    }

    private ObligationDtos.ObligationResponse toObligation(com.lifeos.obligation.domain.Obligation o) {
        return new ObligationDtos.ObligationResponse(o.getId(), o.getHouseholdId(), o.getRecordId(), o.getTitle(), o.getDescription(),
                o.getResponsibleMemberId(), o.getDueAt(), o.getPriority(), o.getStatus(), o.getRecurrenceRule(), o.getSourceType(),
                o.getSourceDocumentId(), o.getCompletedAt(), o.getCancelledAt(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
