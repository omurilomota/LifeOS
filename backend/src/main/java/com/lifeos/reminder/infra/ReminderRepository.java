package com.lifeos.reminder.infra;

import com.lifeos.reminder.domain.Reminder;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {
    boolean existsByDedupeKey(String dedupeKey);
    List<Reminder> findByStatusAndScheduledAtBefore(String status, Instant now);
}
