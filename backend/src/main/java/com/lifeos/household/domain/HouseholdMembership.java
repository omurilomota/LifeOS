package com.lifeos.household.domain;

import com.lifeos.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "household_memberships")
public class HouseholdMembership extends BaseEntity {

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HouseholdRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected HouseholdMembership() {}

    public HouseholdMembership(UUID householdId, UUID userId, HouseholdRole role, Instant joinedAt) {
        this.householdId = householdId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public UUID getHouseholdId() { return householdId; }
    public UUID getUserId() { return userId; }
    public HouseholdRole getRole() { return role; }
    public void setRole(HouseholdRole role) { this.role = role; }
    public Instant getJoinedAt() { return joinedAt; }
}
