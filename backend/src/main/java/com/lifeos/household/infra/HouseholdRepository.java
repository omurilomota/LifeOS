package com.lifeos.household.infra;

import com.lifeos.household.domain.Household;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdRepository extends JpaRepository<Household, UUID> {
}
