package com.lifeos.ai.api;

import com.lifeos.ai.application.AiSuggestionDtos;
import com.lifeos.ai.application.AiSuggestionService;
import com.lifeos.shared.security.SecurityUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AiSuggestionController {

    private final AiSuggestionService aiSuggestionService;

    public AiSuggestionController(AiSuggestionService aiSuggestionService) {
        this.aiSuggestionService = aiSuggestionService;
    }

    @GetMapping("/ai/status")
    public Map<String, Object> status() {
        return Map.of("enabled", aiSuggestionService.isEnabled());
    }

    @PostMapping("/documents/{documentId}/suggestions")
    public List<AiSuggestionDtos.SuggestionResponse> generate(@PathVariable UUID documentId) {
        return aiSuggestionService.generateForDocument(SecurityUtils.currentUserId(), documentId);
    }

    @GetMapping("/documents/{documentId}/suggestions")
    public List<AiSuggestionDtos.SuggestionResponse> list(@PathVariable UUID documentId) {
        return aiSuggestionService.listForDocument(SecurityUtils.currentUserId(), documentId);
    }

    @PostMapping("/ai/suggestions/{id}/confirm")
    public AiSuggestionDtos.ConfirmResponse confirm(@PathVariable UUID id) {
        return aiSuggestionService.confirm(SecurityUtils.currentUserId(), id);
    }

    @PostMapping("/ai/suggestions/{id}/reject")
    public AiSuggestionDtos.SuggestionResponse reject(@PathVariable UUID id) {
        return aiSuggestionService.reject(SecurityUtils.currentUserId(), id);
    }
}
