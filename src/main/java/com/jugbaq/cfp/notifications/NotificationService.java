package com.jugbaq.cfp.notifications;

import com.jugbaq.cfp.notifications.domain.Notification;
import com.jugbaq.cfp.notifications.domain.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<NotificationSummary> listForUser(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    public void markAsRead(UUID userId) {
        repository.markAllAsReadByUserId(userId, Instant.now());
    }

    public void markAllAsRead(UUID userId) {
        repository.markAllAsReadByUserId(userId, Instant.now());
    }

    public void create(UUID userId, UUID tenantId, NotificationType type, Map<String, Object> payload) {
        Map<String, Object> safePayload = (payload == null) ? Map.of() : Map.copyOf(payload);
        repository.save(new Notification(userId, tenantId, type, safePayload));
    }
}
