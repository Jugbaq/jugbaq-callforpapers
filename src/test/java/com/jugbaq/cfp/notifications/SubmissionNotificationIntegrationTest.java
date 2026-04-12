package com.jugbaq.cfp.notifications;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.jugbaq.cfp.TestcontainersConfiguration;
import com.jugbaq.cfp.events.EventService;
import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventStatus;
import com.jugbaq.cfp.notifications.domain.NotificationRepository;
import com.jugbaq.cfp.shared.domain.TenantRepository;
import com.jugbaq.cfp.shared.tenant.TenantContext;
import com.jugbaq.cfp.submissions.SubmissionData;
import com.jugbaq.cfp.submissions.SubmissionService;
import com.jugbaq.cfp.submissions.domain.SubmissionLevel;
import com.jugbaq.cfp.users.UserRegistrationService;
import com.jugbaq.cfp.users.domain.User;
import com.jugbaq.cfp.users.domain.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SubmissionNotificationIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @DynamicPropertySource
    static void mailProps(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> ServerSetupTest.SMTP.getPort());
    }

    @Autowired EventService eventService;
    @Autowired SubmissionService submissionService;
    @Autowired TenantRepository tenantRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    UserRegistrationService userRegistrationService;

    @BeforeEach
    void setUp() {
        var tenant = tenantRepository.findBySlug("jugbaq").orElseThrow();
        TenantContext.set(tenant.getId(), "jugbaq");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void should_send_email_and_persist_notification_when_submission_submitted() throws Exception {
        // Arrange: speaker user
        // 1. Usamos el servicio, que YA le pone el rol de SPEAKER y lo guarda.
        User speaker = userRegistrationService.registerSpeaker(
                "speaker.test." + UUID.randomUUID() + "@jugbaq.dev", // Sugiero añadir UUID por si acaso corres el test 2 veces
                "Test Speaker",
                "pwd123"
        );

        // 2. Creamos el evento
        Event event = eventService.createEvent(
                "notif-test-" + UUID.randomUUID(),
                "Notification Test Event",
                Instant.now().plusSeconds(86400 * 30),
                UUID.fromString("a0000000-0000-0000-0000-000000000001")
        );
        eventService.updateStatus(event.getId(), EventStatus.CFP_OPEN);

        // 3. Preparamos la data
        SubmissionData data = new SubmissionData();
        data.setTitle("Charla de prueba para notificaciones");
        data.setAbstractText("Un abstract lo suficientemente largo para pasar las validaciones del binder.");
        data.setLevel(SubmissionLevel.INTERMEDIATE);

        // Act
        submissionService.createAndSubmit(event.getId(), speaker.getId(), data);

        // Assert — @Async listener, esperamos
        User finalSpeaker = speaker;
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            MimeMessage[] received = greenMail.getReceivedMessages();
            assertThat(received).isNotEmpty();

            boolean speakerEmail = false;
            for (MimeMessage msg : received) {
                if (msg.getAllRecipients()[0].toString().equals(finalSpeaker.getEmail())) {
                    speakerEmail = true;
                    assertThat(msg.getSubject()).contains("Recibimos tu propuesta");
                }
            }
            assertThat(speakerEmail).isTrue();

            assertThat(notificationRepository.countByUserIdAndReadAtIsNull(finalSpeaker.getId()))
                    .isGreaterThanOrEqualTo(1);
        });
    }
}
