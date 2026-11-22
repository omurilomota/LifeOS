package com.lifeos.household.application;

import com.lifeos.household.domain.HouseholdRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class HouseholdDtos {
    private HouseholdDtos() {}

    public record CurrentHouseholdResponse(UUID id, String name, String role) {}
    public record MemberResponse(UUID membershipId, UUID userId, String displayName, String email, HouseholdRole role, Instant joinedAt) {}
    public record InvitationResponse(UUID id, String invitedEmail, HouseholdRole role, Instant expiresAt, Instant acceptedAt, Instant revokedAt, Instant createdAt) {}
    public record HouseholdDetailResponse(UUID id, String name, String myRole, List<MemberResponse> members, List<InvitationResponse> invitations) {}
    public record InviteRequest(@Email @NotBlank String email, @NotNull HouseholdRole role) {}
    public record InviteResponse(InvitationResponse invitation, String token) {}
    public record AcceptInviteRequest(@NotBlank String token) {}
    public record UpdateRoleRequest(@NotNull HouseholdRole role) {}
}
