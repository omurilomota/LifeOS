package com.lifeos.notification.application;

import com.lifeos.household.domain.HouseholdMembership;
import com.lifeos.household.infra.HouseholdMembershipRepository;
import com.lifeos.notification.domain.Notification;
import com.lifeos.notification.infra.NotificationRepository;
import com.lifeos.shared.error.ApiException;
import com.lifeos.shared.time.TimeProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final HouseholdMembershipRepository membershipRepository;
    private final TimeProvider timeProvider;

    public NotificationService(NotificationRepository notificationRepository, HouseholdMembershipRepository membershipRepository, TimeProvider timeProvider) {
        this.notificationRepository = notificationRepository;
        this.membershipRepository = membershipRepository;
        this.timeProvider = timeProvider;
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> list(UUID userId) {
        HouseholdMembership m = membership(userId);
        return notificationRepository.findByUserIdAndHouseholdIdOrderByCreatedAtDesc(userId, m.getHouseholdId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void markAllRead(UUID userId) {
        HouseholdMembership m = membership(userId);
        notificationRepository.findByUserIdAndHouseholdIdOrderByCreatedAtDesc(userId, m.getHouseholdId()).stream()
                .filter(n -> n.getReadAt() == null)
                .forEach(n -> { n.setReadAt(timeProvider.now()); notificationRepository.save(n); });
    }

    @Transactional
    public NotificationDtos.NotificationResponse markRead(UUID userId, UUID notificationId) {
        HouseholdMembership m = membership(userId);
        Notification n = notificationRepository.findByIdAndUserIdAndHouseholdId(notificationId, userId, m.getHouseholdId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification not found"));
        if (n.getReadAt() == null) { n.setReadAt(timeProvider.now()); notificationRepository.save(n); }
        return toResponse(n);
    }

    private HouseholdMembership membership(UUID userId) {
        return membershipRepository.findByUserId(userId).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "HOUSEHOLD_REQUIRED", "Household membership required"));
    }

    private NotificationDtos.NotificationResponse toResponse(Notification n) {
        return new NotificationDtos.NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getReadAt(), n.getRelatedEntityType(), n.getRelatedEntityId(), n.getCreatedAt());
    }
}
