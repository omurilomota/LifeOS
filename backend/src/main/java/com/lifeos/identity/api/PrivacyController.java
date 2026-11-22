package com.lifeos.identity.api;

import com.lifeos.identity.application.DataPrivacyService;
import com.lifeos.shared.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/privacy")
public class PrivacyController {

    private final DataPrivacyService dataPrivacyService;

    public PrivacyController(DataPrivacyService dataPrivacyService) {
        this.dataPrivacyService = dataPrivacyService;
    }

    @GetMapping("/export")
    public Map<String, Object> export() {
        return dataPrivacyService.exportData(SecurityUtils.currentUserId());
    }

    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(HttpServletRequest request) {
        dataPrivacyService.deleteAccount(SecurityUtils.currentUserId());
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}
