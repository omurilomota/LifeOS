package com.lifeos.household.infra;

import com.lifeos.household.domain.HouseholdInvitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdInvitationRepository extends JpaRepository<HouseholdInvitation, UUID> {
    List<HouseholdInvitation> findByHouseholdIdOrderByCreatedAtDesc(UUID householdId);
    Optional<HouseholdInvitation> findByTokenHash(String tokenHash);
}
