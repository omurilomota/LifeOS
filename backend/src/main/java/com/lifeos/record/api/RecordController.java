package com.lifeos.record.api;

import com.lifeos.record.application.RecordDtos;
import com.lifeos.record.application.RecordService;
import com.lifeos.record.domain.RecordStatus;
import com.lifeos.record.domain.RecordType;
import com.lifeos.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    public List<RecordDtos.RecordSummaryResponse> list(
            @RequestParam(required = false) RecordStatus status,
            @RequestParam(required = false) RecordType type,
            @RequestParam(required = false) String q) {
        return recordService.list(SecurityUtils.currentUserId(), status, type, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecordDtos.RecordResponse create(@Valid @RequestBody RecordDtos.RecordRequest request) {
        return recordService.create(SecurityUtils.currentUserId(), request);
    }

    @GetMapping("/{id}")
    public RecordDtos.RecordDetailResponse get(@PathVariable UUID id) {
        return recordService.get(SecurityUtils.currentUserId(), id);
    }

    @PutMapping("/{id}")
    public RecordDtos.RecordResponse update(@PathVariable UUID id, @Valid @RequestBody RecordDtos.RecordRequest request) {
        return recordService.update(SecurityUtils.currentUserId(), id, request);
    }

    @PatchMapping("/{id}/archive")
    public RecordDtos.RecordResponse archive(@PathVariable UUID id) {
        return recordService.archive(SecurityUtils.currentUserId(), id);
    }

    @PostMapping("/{id}/relationships")
    @ResponseStatus(HttpStatus.CREATED)
    public RecordDtos.RelationshipResponse addRelationship(@PathVariable UUID id, @Valid @RequestBody RecordDtos.RelationshipRequest request) {
        return recordService.addRelationship(SecurityUtils.currentUserId(), id, request);
    }

    @GetMapping("/{id}/history")
    public List<RecordDtos.AuditEventResponse> history(@PathVariable UUID id) {
        return recordService.history(SecurityUtils.currentUserId(), id);
    }
}
