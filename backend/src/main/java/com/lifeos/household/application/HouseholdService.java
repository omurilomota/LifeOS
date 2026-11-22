package com.lifeos.household.application;

import com.lifeos.activity.application.AuditService;
import com.lifeos.household.domain.Household;
import com.lifeos.household.domain.HouseholdInvitation;
import com.lifeos.household.domain.HouseholdMembership;
import com.lifeos.household.domain.HouseholdRole;
import com.lifeos.household.infra.HouseholdInvitationRepository;
import com.lifeos.household.infra.HouseholdMembershipRepository;
import com.lifeos.household.infra.HouseholdRepository;
import com.lifeos.identity.domain.User;
import com.lifeos.identity.infra.UserRepository;
import com.lifeos.shared.error.ApiException;
import com.lifeos.shared.time.TimeProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HouseholdService {
    private static final Duration INVITE_TTL = Duration.ofDays(7);

    private final HouseholdRepository householdRepository;
    private final HouseholdMembershipRepository membershipRepository;
    private final HouseholdInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    public HouseholdService(HouseholdRepository householdRepository, HouseholdMembershipRepository membershipRepository,
                            HouseholdInvitationRepository invitationRepository, UserRepository userRepository,
                            AuditService auditService, TimeProvider timeProvider) {
        this.householdRepository = householdRepository;
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    @Transactional(readOnly = true)
    public HouseholdDtos.CurrentHouseholdResponse getCurrentHousehold(UUID userId) {
        HouseholdMembership membership = requireMembership(userId);
        Household household = householdRepository.findById(membership.getHouseholdId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HOUSEHOLD_NOT_FOUND", "Household not found"));
        return new HouseholdDtos.CurrentHouseholdResponse(household.getId(), household.getName(), membership.getRole().name());
    }

    @Transactional(readOnly = true)
    public HouseholdDtos.HouseholdDetailResponse getDetail(UUID userId) {
        HouseholdMembership membership = requireMembership(userId);
        Household household = householdRepository.findById(membership.getHouseholdId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HOUSEHOLD_NOT_FOUND", "Household not found"));
        List<HouseholdDtos.MemberResponse> members = membershipRepository.findByHouseholdId(household.getId()).stream().map(m -> {
            User user = userRepository.findById(m.getUserId()).orElse(null);
            String name = user == null ? "Unknown" : user.getDisplayName();
            String email = user == null ? "" : user.getEmail();
            return new HouseholdDtos.MemberResponse(m.getId(), m.getUserId(), name, email, m.getRole(), m.getJoinedAt());
        }).toList();
        List<HouseholdDtos.InvitationResponse> invites = invitationRepository.findByHouseholdIdOrderByCreatedAtDesc(household.getId())
                .stream().map(this::toInvite).toList();
        return new HouseholdDtos.HouseholdDetailResponse(household.getId(), household.getName(), membership.getRole().name(), members, invites);
    }

    @Transactional
    public HouseholdDtos.InviteResponse invite(UUID userId, HouseholdDtos.InviteRequest request) {
        HouseholdMembership actor = requireMembership(userId);
        requireAdmin(actor);
        if (request.role() == HouseholdRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "Cannot invite as OWNER");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        Instant now = timeProvider.now();
        HouseholdInvitation inv = new HouseholdInvitation(actor.getHouseholdId(), email, sha256(token), request.role(), now.plus(INVITE_TTL), userId);
        invitationRepository.save(inv);
        auditService.record(actor.getHouseholdId(), userId, "HOUSEHOLD_INVITATION", inv.getId(), "INVITATION_CREATED", java.util.Map.of("email", email, "role", request.role().name()));
        return new HouseholdDtos.InviteResponse(toInvite(inv), token);
    }

    @Transactional
    public HouseholdDtos.CurrentHouseholdResponse acceptInvite(UUID userId, String rawToken) {
        requireMembership(userId);
        HouseholdInvitation inv = invitationRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INVITE", "Invalid or expired invite"));
        Instant now = timeProvider.now();
        if (!inv.isOpen(now)) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INVITE", "Invalid or expired invite");
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"));
        if (!user.getEmail().equalsIgnoreCase(inv.getInvitedEmail())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INVITE_EMAIL_MISMATCH", "Invite email does not match current user");
        }
        membershipRepository.findByHouseholdIdAndUserId(inv.getHouseholdId(), userId).ifPresent(m -> {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_MEMBER", "User already member of household");
        });
        HouseholdMembership nm = new HouseholdMembership(inv.getHouseholdId(), userId, inv.getRole(), now);
        membershipRepository.save(nm);
        inv.setAcceptedAt(now);
        invitationRepository.save(inv);
        auditService.record(inv.getHouseholdId(), userId, "HOUSEHOLD_INVITATION", inv.getId(), "INVITATION_ACCEPTED", java.util.Map.of());
        Household household = householdRepository.findById(inv.getHouseholdId()).orElseThrow();
        return new HouseholdDtos.CurrentHouseholdResponse(household.getId(), household.getName(), nm.getRole().name());
    }

    @Transactional
    public void revokeInvite(UUID userId, UUID invitationId) {
        HouseholdMembership actor = requireMembership(userId);
        requireAdmin(actor);
        HouseholdInvitation inv = invitationRepository.findById(invitationId)
                .filter(i -> i.getHouseholdId().equals(actor.getHouseholdId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "Invitation not found"));
        if (inv.getRevokedAt() == null && inv.getAcceptedAt() == null) {
            inv.setRevokedAt(timeProvider.now());
            invitationRepository.save(inv);
            auditService.record(actor.getHouseholdId(), userId, "HOUSEHOLD_INVITATION", inv.getId(), "INVITATION_REVOKED", java.util.Map.of());
        }
    }

    @Transactional
    public HouseholdDtos.MemberResponse updateMemberRole(UUID userId, UUID membershipId, HouseholdRole role) {
        HouseholdMembership actor = requireMembership(userId);
        requireOwner(actor);
        if (role == HouseholdRole.OWNER) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "Cannot assign OWNER role");
        HouseholdMembership target = membershipRepository.findById(membershipId)
                .filter(m -> m.getHouseholdId().equals(actor.getHouseholdId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found"));
        if (target.getUserId().equals(userId)) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OPERATION", "Cannot change own role");
        target.setRole(role);
        membershipRepository.save(target);
        auditService.record(actor.getHouseholdId(), userId, "HOUSEHOLD_MEMBER", target.getId(), "MEMBER_ROLE_UPDATED", java.util.Map.of("role", role.name()));
        User user = userRepository.findById(target.getUserId()).orElse(null);
        return new HouseholdDtos.MemberResponse(target.getId(), target.getUserId(), user == null ? "Unknown" : user.getDisplayName(), user == null ? "" : user.getEmail(), target.getRole(), target.getJoinedAt());
    }

    @Transactional
    public void removeMember(UUID userId, UUID membershipId) {
        HouseholdMembership actor = requireMembership(userId);
        requireOwner(actor);
        HouseholdMembership target = membershipRepository.findById(membershipId)
                .filter(m -> m.getHouseholdId().equals(actor.getHouseholdId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found"));
        if (target.getUserId().equals(userId)) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OPERATION", "Cannot remove yourself");
        if (target.getRole() == HouseholdRole.OWNER) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OPERATION", "Cannot remove owner");
        membershipRepository.delete(target);
        auditService.record(actor.getHouseholdId(), userId, "HOUSEHOLD_MEMBER", target.getId(), "MEMBER_REMOVED", java.util.Map.of());
    }

    private HouseholdMembership requireMembership(UUID userId) {
        return membershipRepository.findByUserId(userId).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HOUSEHOLD_NOT_FOUND", "Household not found"));
    }

    private void requireAdmin(HouseholdMembership actor) {
        if (actor.getRole() != HouseholdRole.OWNER && actor.getRole() != HouseholdRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Insufficient role");
        }
    }

    private void requireOwner(HouseholdMembership actor) {
        if (actor.getRole() != HouseholdRole.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Owner role required");
        }
    }

    private HouseholdDtos.InvitationResponse toInvite(HouseholdInvitation i) {
        return new HouseholdDtos.InvitationResponse(i.getId(), i.getInvitedEmail(), i.getRole(), i.getExpiresAt(), i.getAcceptedAt(), i.getRevokedAt(), i.getCreatedAt());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
