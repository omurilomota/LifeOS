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
@Table(name = "household_invitations")
public class HouseholdInvitation extends BaseEntity {

    @Column(name = "household_id", nullable = false)
    private UUID householdId;
    @Column(name = "invited_email", nullable = false, length = 320)
    private String invitedEmail;
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HouseholdRole role;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "accepted_at")
    private Instant acceptedAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    protected HouseholdInvitation() {}

    public HouseholdInvitation(UUID householdId, String invitedEmail, String tokenHash, HouseholdRole role, Instant expiresAt, UUID createdByUserId) {
        this.householdId = householdId;
        this.invitedEmail = invitedEmail;
        this.tokenHash = tokenHash;
        this.role = role;
        this.expiresAt = expiresAt;
        this.createdByUserId = createdByUserId;
    }

    public UUID getHouseholdId() { return householdId; }
    public String getInvitedEmail() { return invitedEmail; }
    public String getTokenHash() { return tokenHash; }
    public HouseholdRole getRole() { return role; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public boolean isOpen(Instant now) { return acceptedAt == null && revokedAt == null && expiresAt.isAfter(now); }
}
