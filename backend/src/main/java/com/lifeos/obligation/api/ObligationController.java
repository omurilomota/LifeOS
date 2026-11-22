package com.lifeos.obligation.api;

import com.lifeos.obligation.application.ObligationDtos;
import com.lifeos.obligation.application.ObligationService;
import com.lifeos.obligation.domain.ObligationStatus;
import com.lifeos.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/obligations")
public class ObligationController {
    private final ObligationService obligationService;
    public ObligationController(ObligationService obligationService) { this.obligationService = obligationService; }

    @GetMapping
    public List<ObligationDtos.ObligationResponse> list(@RequestParam(required = false) ObligationStatus status) {
        return obligationService.list(SecurityUtils.currentUserId(), status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ObligationDtos.ObligationResponse create(@Valid @RequestBody ObligationDtos.ObligationRequest request) {
        return obligationService.create(SecurityUtils.currentUserId(), request);
    }

    @GetMapping("/{id}")
    public ObligationDtos.ObligationResponse get(@PathVariable UUID id) {
        return obligationService.get(SecurityUtils.currentUserId(), id);
    }

    @PutMapping("/{id}")
    public ObligationDtos.ObligationResponse update(@PathVariable UUID id, @Valid @RequestBody ObligationDtos.ObligationRequest request) {
        return obligationService.update(SecurityUtils.currentUserId(), id, request);
    }

    @PatchMapping("/{id}/complete")
    public ObligationDtos.ObligationResponse complete(@PathVariable UUID id) {
        return obligationService.complete(SecurityUtils.currentUserId(), id);
    }

    @PatchMapping("/{id}/cancel")
    public ObligationDtos.ObligationResponse cancel(@PathVariable UUID id) {
        return obligationService.cancel(SecurityUtils.currentUserId(), id);
    }

    @PatchMapping("/{id}/reschedule")
    public ObligationDtos.ObligationResponse reschedule(@PathVariable UUID id, @Valid @RequestBody ObligationDtos.RescheduleRequest request) {
        return obligationService.reschedule(SecurityUtils.currentUserId(), id, request.dueAt());
    }
}
