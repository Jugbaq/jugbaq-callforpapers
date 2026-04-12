package com.jugbaq.cfp.notifications;

import com.jugbaq.cfp.notifications.domain.Notification;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationSummary(
        UUID id,
        NotificationType type,
        Map<String, Object> payload,
        Instant createdAt,
        boolean isRead
) {
    public static NotificationSummary from(Notification n) {
        return new NotificationSummary(
                n.getId(), n.getType(), n.getPayload(), n.getCreatedAt(), n.getReadAt() != null
        );
    }
}