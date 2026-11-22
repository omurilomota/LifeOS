package com.lifeos.household.infra;

import com.lifeos.household.domain.HouseholdMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdMembershipRepository extends JpaRepository<HouseholdMembership, UUID> {
    List<HouseholdMembership> findByUserId(UUID userId);
    Optional<HouseholdMembership> findByHouseholdIdAndUserId(UUID householdId, UUID userId);
    List<HouseholdMembership> findByHouseholdId(UUID householdId);
}
