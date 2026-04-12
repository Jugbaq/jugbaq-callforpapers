package com.jugbaq.cfp.notifications;

import com.jugbaq.cfp.notifications.domain.Notification;
import com.jugbaq.cfp.notifications.domain.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<NotificationSummary> listForUser(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
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
}
