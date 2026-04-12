package com.jugbaq.cfp.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.jugbaq.cfp.TestcontainersConfiguration;
import com.jugbaq.cfp.notifications.NotificationType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Transactional
class NotificationRepositoryTest {

    @Autowired
    NotificationRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void should_save_and_count_unread_notifications() {
        UUID userId = UUID.randomUUID();
        insertDummyUser(userId);

        repository.save(
                new Notification(userId, null, NotificationType.SUBMISSION_RECEIVED, Map.of("title", "Charla 1")));
        repository.save(
                new Notification(userId, null, NotificationType.SUBMISSION_RECEIVED, Map.of("title", "Charla 2")));

        assertThat(repository.countByUserIdAndReadAtIsNull(userId)).isEqualTo(2);
        assertThat(repository.findByUserIdOrderByCreatedAtDesc(userId)).hasSize(2);
    }

    @Test
    void should_mark_as_read_and_decrement_unread_count() {
        UUID userId = UUID.randomUUID();
        insertDummyUser(userId);

        var saved = repository.save(
                new Notification(userId, null, NotificationType.SUBMISSION_RECEIVED, Map.of("title", "X")));

        saved.markAsRead();
        repository.save(saved);

        assertThat(repository.countByUserIdAndReadAtIsNull(userId)).isZero();
    }

    private void insertDummyUser(UUID userId) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, full_name, email_verified, status, created_at, updated_at) "
                        + "VALUES (?, ?, 'Test Speaker', true, 'ACTIVE', NOW(), NOW())",
                userId,
                userId.toString() + "@test.com");
    }
}
