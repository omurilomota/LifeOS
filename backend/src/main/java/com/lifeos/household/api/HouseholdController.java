package com.lifeos.household.api;

import com.lifeos.household.application.HouseholdDtos;
import com.lifeos.household.application.HouseholdService;
import com.lifeos.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households")
public class HouseholdController {

    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @GetMapping("/current")
    public HouseholdDtos.CurrentHouseholdResponse current() {
        return householdService.getCurrentHousehold(SecurityUtils.currentUserId());
    }

    @GetMapping("/current/detail")
    public HouseholdDtos.HouseholdDetailResponse detail() {
        return householdService.getDetail(SecurityUtils.currentUserId());
    }

    @PostMapping("/current/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdDtos.InviteResponse invite(@Valid @RequestBody HouseholdDtos.InviteRequest request) {
        return householdService.invite(SecurityUtils.currentUserId(), request);
    }

    @PostMapping("/invitations/accept")
    public HouseholdDtos.CurrentHouseholdResponse accept(@Valid @RequestBody HouseholdDtos.AcceptInviteRequest request) {
        return householdService.acceptInvite(SecurityUtils.currentUserId(), request.token());
    }

    @PostMapping("/current/invitations/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id) {
        householdService.revokeInvite(SecurityUtils.currentUserId(), id);
    }

    @PatchMapping("/current/members/{membershipId}/role")
    public HouseholdDtos.MemberResponse updateRole(@PathVariable UUID membershipId, @Valid @RequestBody HouseholdDtos.UpdateRoleRequest request) {
        return householdService.updateMemberRole(SecurityUtils.currentUserId(), membershipId, request.role());
    }

    @DeleteMapping("/current/members/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID membershipId) {
        householdService.removeMember(SecurityUtils.currentUserId(), membershipId);
    }
}
