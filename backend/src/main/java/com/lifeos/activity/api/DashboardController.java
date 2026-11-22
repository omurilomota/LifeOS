package com.lifeos.activity.api;

import com.lifeos.activity.application.DashboardDtos;
import com.lifeos.activity.application.DashboardService;
import com.lifeos.shared.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }

    @GetMapping
    public DashboardDtos.DashboardResponse get() {
        return dashboardService.get(SecurityUtils.currentUserId());
    }
}
