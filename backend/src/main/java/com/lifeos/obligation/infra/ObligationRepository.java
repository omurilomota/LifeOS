package com.lifeos.obligation.infra;

import com.lifeos.obligation.domain.Obligation;
import com.lifeos.obligation.domain.ObligationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObligationRepository extends JpaRepository<Obligation, UUID> {
    Optional<Obligation> findByIdAndHouseholdId(UUID id, UUID householdId);

    @Query("""
            select o from Obligation o
            where o.householdId = :householdId
              and (:status is null or o.status = :status)
            order by o.dueAt asc
            """)
    List<Obligation> list(@Param("householdId") UUID householdId, @Param("status") ObligationStatus status);

    List<Obligation> findByHouseholdIdAndStatusAndDueAtBeforeOrderByDueAtAsc(UUID householdId, ObligationStatus status, Instant before);
    List<Obligation> findByHouseholdIdAndStatusAndDueAtBetweenOrderByDueAtAsc(UUID householdId, ObligationStatus status, Instant from, Instant to);
    List<Obligation> findByHouseholdIdAndTitleContainingIgnoreCase(UUID householdId, String title);
}
