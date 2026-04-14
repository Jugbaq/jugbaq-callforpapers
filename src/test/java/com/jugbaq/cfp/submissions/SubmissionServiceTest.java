package com.jugbaq.cfp.submissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jugbaq.cfp.TestcontainersConfiguration;
import com.jugbaq.cfp.events.EventService;
import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventStatus;
import com.jugbaq.cfp.shared.domain.TenantRepository;
import com.jugbaq.cfp.shared.tenant.TenantContext;
import com.jugbaq.cfp.submissions.domain.CfpClosedException;
import com.jugbaq.cfp.submissions.domain.NotSubmissionOwnerException;
import com.jugbaq.cfp.submissions.domain.Submission;
import com.jugbaq.cfp.submissions.domain.SubmissionLevel;
import com.jugbaq.cfp.submissions.domain.SubmissionLimitExceededException;
import com.jugbaq.cfp.submissions.domain.SubmissionStatus;
import com.jugbaq.cfp.submissions.events.SubmissionSubmittedEvent;
import com.jugbaq.cfp.users.domain.User;
import com.jugbaq.cfp.users.domain.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@RecordApplicationEvents
@Transactional
class SubmissionServiceTest {

    @Autowired
    SubmissionService submissionService;

    @Autowired
    EventService eventService;

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    ApplicationEvents applicationEvents;

    @Autowired
    UserRepository userRepository;

    private static final UUID ADMIN_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private UUID speakerId;
    private Event openEvent;

    @BeforeEach
    void setUp() {
        var tenant = tenantRepository.findBySlug("jugbaq").orElseThrow();
        TenantContext.set(tenant.getId(), "jugbaq");

        // 1. Crear un usuario real en base de datos
        User speakerUser = new User("test.speaker." + UUID.randomUUID() + "@jugbaq.dev", "Test Speaker");
        speakerUser = userRepository.save(speakerUser);

        // 2. Usar su ID real
        speakerId = speakerUser.getId();

        openEvent = eventService.createEvent(
                "sub-test-" + UUID.randomUUID(),
                "Submission Test Event",
                Instant.now().plusSeconds(86400 * 30),
                ADMIN_ID);
        eventService.updateStatus(openEvent.getId(), EventStatus.CFP_OPEN);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void should_create_draft_when_cfp_open() {
        SubmissionData data = buildData("Mi charla sobre Kotlin");

        Submission draft = submissionService.createDraft(openEvent.getId(), speakerId, data);

        assertThat(draft.getId()).isNotNull();
        assertThat(draft.getStatus()).isEqualTo(SubmissionStatus.DRAFT);
        assertThat(draft.getTitle()).isEqualTo("Mi charla sobre Kotlin");
        assertThat(draft.getSpeakerId()).isEqualTo(speakerId);
    }

    @Test
    void should_reject_when_cfp_not_open() {
        Event draftEvent = eventService.createEvent(
                "closed-" + UUID.randomUUID(), "Closed", Instant.now().plusSeconds(86400), ADMIN_ID);
        // Queda en DRAFT, CFP no abierto

        assertThatThrownBy(() -> submissionService.createDraft(draftEvent.getId(), speakerId, buildData("X")))
                .isInstanceOf(CfpClosedException.class);
    }

    @Test
    void should_enforce_max_submissions_per_speaker() {
        // Por defecto maxSubmissionsPerSpeaker = 3
        submissionService.createDraft(openEvent.getId(), speakerId, buildData("Charla 1"));
        submissionService.createDraft(openEvent.getId(), speakerId, buildData("Charla 2"));
        submissionService.createDraft(openEvent.getId(), speakerId, buildData("Charla 3"));

        assertThatThrownBy(() -> submissionService.createDraft(openEvent.getId(), speakerId, buildData("Charla 4")))
                .isInstanceOf(SubmissionLimitExceededException.class);
    }

    @Test
    void withdrawn_submissions_do_not_count_toward_limit() {
        Submission first = submissionService.createDraft(openEvent.getId(), speakerId, buildData("C1"));
        submissionService.createDraft(openEvent.getId(), speakerId, buildData("C2"));
        submissionService.createDraft(openEvent.getId(), speakerId, buildData("C3"));

        submissionService.withdraw(first.getId(), speakerId);

        // Ahora debe poder crear una más
        Submission fourth = submissionService.createDraft(openEvent.getId(), speakerId, buildData("C4"));
        assertThat(fourth.getId()).isNotNull();
    }

    @Test
    void should_publish_event_when_submitted() {
        Submission draft = submissionService.createDraft(openEvent.getId(), speakerId, buildData("Evento publicado"));

        submissionService.markAsSubmitted(draft.getId(), speakerId);

        long eventCount = applicationEvents.stream(SubmissionSubmittedEvent.class)
                .filter(e -> e.submissionId().equals(draft.getId()))
                .count();
        assertThat(eventCount).isEqualTo(1);
    }

    @Test
    void should_reject_withdraw_when_not_owner() {
        Submission draft = submissionService.createDraft(openEvent.getId(), speakerId, buildData("Mía"));
        UUID otroSpeaker = UUID.randomUUID();

        assertThatThrownBy(() -> submissionService.withdraw(draft.getId(), otroSpeaker))
                .isInstanceOf(NotSubmissionOwnerException.class);
    }

    @Test
    void should_update_draft_content() {
        Submission draft = submissionService.createDraft(openEvent.getId(), speakerId, buildData("Título original"));

        SubmissionData updated = buildData("Título actualizado");
        updated.setAbstractText("Nuevo abstract mucho más detallado");
        submissionService.updateDraft(draft.getId(), speakerId, updated);

        Submission reloaded = submissionService.findById(draft.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Título actualizado");
        assertThat(reloaded.getAbstractText()).contains("más detallado");
    }

    @Test
    void should_list_for_review_with_event_and_status() {
        Submission draft = submissionService.createDraft(openEvent.getId(), speakerId, buildData("Review 1"));
        submissionService.markAsSubmitted(draft.getId(), speakerId);

        List<Submission> result = submissionService.listForReview(openEvent.getId(), SubmissionStatus.SUBMITTED);
        assertThat(result).isNotEmpty();
    }

    @Test
    void should_list_for_review_with_event_only() {
        Submission draft = submissionService.createDraft(openEvent.getId(), speakerId, buildData("Review 2"));
        submissionService.markAsSubmitted(draft.getId(), speakerId);

        List<Submission> result = submissionService.listForReview(openEvent.getId(), null);
        assertThat(result).isNotEmpty();
    }

    @Test
    void should_list_for_review_with_status_only() {
        Submission draft = submissionService.createDraft(openEvent.getId(), speakerId, buildData("Review 3"));
        submissionService.markAsSubmitted(draft.getId(), speakerId);

        List<Submission> result = submissionService.listForReview(null, SubmissionStatus.SUBMITTED);
        assertThat(result).isNotEmpty();
    }

    @Test
    void should_list_for_review_with_no_filters() {
        Submission draft = submissionService.createDraft(openEvent.getId(), speakerId, buildData("Review 4"));
        submissionService.markAsSubmitted(draft.getId(), speakerId);

        List<Submission> result = submissionService.listForReview(null, null);
        assertThat(result).isNotEmpty();
    }

    @Test
    void should_count_active_submissions_by_speaker() {
        submissionService.createDraft(openEvent.getId(), speakerId, buildData("Count 1"));
        submissionService.createDraft(openEvent.getId(), speakerId, buildData("Count 2"));

        long count = submissionService.countActiveSubmissionsBySpeaker(openEvent.getId(), speakerId);
        assertThat(count).isEqualTo(2);
    }

    private SubmissionData buildData(String title) {
        SubmissionData data = new SubmissionData();
        data.setTitle(title);
        data.setAbstractText("Un abstract de prueba para la charla con suficiente contenido.");
        data.setPitch("Por qué esta charla importa");
        data.setLevel(SubmissionLevel.INTERMEDIATE);
        return data;
    }
}
