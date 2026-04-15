package com.jugbaq.cfp.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import com.jugbaq.cfp.TestcontainersConfiguration;
import com.jugbaq.cfp.notifications.domain.Notification;
import com.jugbaq.cfp.notifications.domain.NotificationRepository;
import com.jugbaq.cfp.shared.domain.TenantRepository;
import com.jugbaq.cfp.users.domain.User;
import com.jugbaq.cfp.users.domain.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class NotificationServiceTest {

    @Autowired
    NotificationService notificationService;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TenantRepository tenantRepository;

    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        var tenant = tenantRepository.findBySlug("jugbaq").orElseThrow();
        tenantId = tenant.getId();

        User user = new User("notif.test@jugbaq.dev", "Notification Test");
        user.setPasswordHash("$2a$10$fake");
        user = userRepository.save(user);
        userId = user.getId();
    }

    @Test
    void should_create_notification_with_payload() {
        Map<String, Object> payload = Map.of("key", "value");

        notificationService.create(userId, tenantId, NotificationType.SUBMISSION_ACCEPTED, payload);

        var notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);

        Notification saved = notifications.getFirst();
        assertThat(saved).extracting("userId").isEqualTo(userId);
        assertThat(saved).extracting("tenantId").isEqualTo(tenantId);
        assertThat(saved).extracting("type").isEqualTo(NotificationType.SUBMISSION_ACCEPTED);
        assertThat(saved).extracting("payload").isEqualTo(payload);
    }

    @Test
    void should_create_notification_with_null_payload() {
        notificationService.create(userId, tenantId, NotificationType.SUBMISSION_ACCEPTED, null);

        var notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);

        Notification saved = notifications.getFirst();
        assertThat(saved).extracting("userId").isEqualTo(userId);
        assertThat(saved).extracting("type").isEqualTo(NotificationType.SUBMISSION_ACCEPTED);
        assertThat(saved).extracting("payload").isEqualTo(Map.of());
    }

    @Test
    void should_list_notifications_for_user() {
        Notification notif =
                new Notification(userId, tenantId, NotificationType.SUBMISSION_ACCEPTED, Map.of("title", "Test"));
        notificationRepository.save(notif);

        var result = notificationService.listForUser(userId);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(NotificationType.SUBMISSION_ACCEPTED);
    }

    @Test
    void should_count_unread_notifications() {
        Notification notif1 = new Notification(userId, tenantId, NotificationType.SUBMISSION_ACCEPTED, Map.of());
        Notification notif2 = new Notification(userId, tenantId, NotificationType.SUBMISSION_REJECTED, Map.of());
        notificationRepository.save(notif1);
        notificationRepository.save(notif2);

        long count = notificationService.unreadCount(userId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void should_mark_all_as_read() {
        Notification notif1 = new Notification(userId, tenantId, NotificationType.SUBMISSION_ACCEPTED, Map.of());
        notificationRepository.save(notif1);

        long before = notificationService.unreadCount(userId);
        assertThat(before).isEqualTo(1);

        notificationService.markAllAsRead(userId);

        long after = notificationService.unreadCount(userId);
        assertThat(after).isEqualTo(0);
    }

    @Test
    void should_mark_as_read() {
        Notification notif = new Notification(userId, tenantId, NotificationType.SUBMISSION_ACCEPTED, Map.of());
        notificationRepository.save(notif);

        long before = notificationService.unreadCount(userId);
        assertThat(before).isEqualTo(1);

        notificationService.markAsRead(userId);

        long after = notificationService.unreadCount(userId);
        assertThat(after).isEqualTo(0);
    }

    @Test
    void should_return_empty_list_for_user_with_no_notifications() {
        var result = notificationService.listForUser(userId);
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_zero_unread_for_user_with_no_notifications() {
        long count = notificationService.unreadCount(userId);
        assertThat(count).isEqualTo(0);
    }
}
