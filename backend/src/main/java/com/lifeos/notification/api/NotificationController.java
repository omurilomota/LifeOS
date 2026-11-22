package com.lifeos.notification.api;

import com.lifeos.notification.application.NotificationDtos;
import com.lifeos.notification.application.NotificationService;
import com.lifeos.shared.security.SecurityUtils;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    public NotificationController(NotificationService notificationService) { this.notificationService = notificationService; }

    @GetMapping
    public List<NotificationDtos.NotificationResponse> list() {
        return notificationService.list(SecurityUtils.currentUserId());
    }

    @PatchMapping("/{id}/read")
    public NotificationDtos.NotificationResponse markRead(@PathVariable UUID id) {
        return notificationService.markRead(SecurityUtils.currentUserId(), id);
    }

    @PatchMapping("/read-all")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void markAllRead() {
        notificationService.markAllRead(SecurityUtils.currentUserId());
    }
}
