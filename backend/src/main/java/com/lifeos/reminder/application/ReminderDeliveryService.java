package com.lifeos.reminder.application;

import com.lifeos.household.infra.HouseholdMembershipRepository;
import com.lifeos.notification.domain.Notification;
import com.lifeos.notification.infra.NotificationRepository;
import com.lifeos.obligation.domain.Obligation;
import com.lifeos.obligation.domain.ObligationStatus;
import com.lifeos.obligation.infra.ObligationRepository;
import com.lifeos.reminder.domain.Reminder;
import com.lifeos.reminder.infra.ReminderRepository;
import com.lifeos.shared.observability.LifeOsMetrics;
import com.lifeos.shared.time.TimeProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(ReminderDeliveryService.class);

    private final ReminderRepository reminderRepository;
    private final ObligationRepository obligationRepository;
    private final NotificationRepository notificationRepository;
    private final HouseholdMembershipRepository membershipRepository;
    private final TimeProvider timeProvider;
    private final LifeOsMetrics metrics;

    public ReminderDeliveryService(ReminderRepository reminderRepository, ObligationRepository obligationRepository,
                                   NotificationRepository notificationRepository, HouseholdMembershipRepository membershipRepository,
                                   TimeProvider timeProvider, LifeOsMetrics metrics) {
        this.reminderRepository = reminderRepository;
        this.obligationRepository = obligationRepository;
        this.notificationRepository = notificationRepository;
        this.membershipRepository = membershipRepository;
        this.timeProvider = timeProvider;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${lifeos.reminders.poll-ms:60000}")
    @Transactional
    public void deliverDueReminders() {
        Instant now = timeProvider.now();
        List<Reminder> due = reminderRepository.findByStatusAndScheduledAtBefore("PENDING", now);
        for (Reminder reminder : due) {
            deliver(reminder, now);
        }
        if (!due.isEmpty()) log.info("Processed {} due reminders", due.size());
    }

    private void deliver(Reminder reminder, Instant now) {
        Obligation obligation = obligationRepository.findById(reminder.getObligationId()).orElse(null);
        if (obligation == null || obligation.getStatus() != ObligationStatus.OPEN) {
            reminder.setStatus("CANCELLED");
            reminder.setDeliveredAt(now);
            reminderRepository.save(reminder);
            metrics.reminder("cancelled");
            return;
        }
        membershipRepository.findByHouseholdId(obligation.getHouseholdId()).forEach(member -> {
            boolean exists = notificationRepository.existsByUserIdAndRelatedEntityTypeAndRelatedEntityIdAndType(
                    member.getUserId(), "OBLIGATION", obligation.getId(), "REMINDER_DUE");
            if (!exists) {
                notificationRepository.save(new Notification(UUID.randomUUID(), obligation.getHouseholdId(), member.getUserId(),
                        "REMINDER_DUE", "Lembrete: " + obligation.getTitle(),
                        "A obrigação vence em " + obligation.getDueAt(), "OBLIGATION", obligation.getId(), now));
            } else {
                metrics.reminder("deduplicated");
            }
        });
        reminder.setStatus("DELIVERED");
        reminder.setDeliveredAt(now);
        reminderRepository.save(reminder);
        metrics.reminder("delivered");
    }
}
