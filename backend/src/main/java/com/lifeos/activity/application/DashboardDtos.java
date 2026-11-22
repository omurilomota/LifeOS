package com.lifeos.activity.application;

import com.lifeos.notification.application.NotificationDtos;
import com.lifeos.obligation.application.ObligationDtos;
import com.lifeos.record.application.RecordDtos;
import java.util.List;

public class DashboardDtos {
    private DashboardDtos() {}
    public record DashboardResponse(
            List<ObligationDtos.ObligationResponse> overdue,
            List<ObligationDtos.ObligationResponse> dueSoon,
            List<RecordDtos.RecordSummaryResponse> recentRecords,
            List<NotificationDtos.NotificationResponse> notifications
    ) {}
}
