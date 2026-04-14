package com.jugbaq.cfp.publishing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jugbaq.cfp.TestcontainersConfiguration;
import com.jugbaq.cfp.events.EventService;
import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventStatus;
import com.jugbaq.cfp.publishing.domain.AgendaConflictException;
import com.jugbaq.cfp.publishing.domain.AgendaIncompleteException;
import com.jugbaq.cfp.publishing.domain.AgendaSlot;
import com.jugbaq.cfp.review.ReviewService;
import com.jugbaq.cfp.shared.domain.TenantRepository;
import com.jugbaq.cfp.shared.tenant.TenantContext;
import com.jugbaq.cfp.submissions.SubmissionData;
import com.jugbaq.cfp.submissions.SubmissionService;
import com.jugbaq.cfp.submissions.domain.Submission;
import com.jugbaq.cfp.submissions.domain.SubmissionLevel;
import com.jugbaq.cfp.users.UserRegistrationService;
import com.jugbaq.cfp.users.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AgendaServiceTest {

    @Autowired
    AgendaService agendaService;

    @Autowired
    EventService eventService;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    ReviewService reviewService;

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    UserRegistrationService userRegistrationService;

    private static final UUID ADMIN_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private Event event;
    private Submission accepted1;
    private Submission accepted2;
    private UUID speakerA;
    private UUID speakerB;

    @BeforeEach
    void setUp() {
        var tenant = tenantRepository.findBySlug("jugbaq").orElseThrow();
        TenantContext.set(tenant.getId(), "jugbaq");

        event = eventService.createEvent(
                "agenda-" + UUID.randomUUID(), "Agenda Test", Instant.now().plus(30, ChronoUnit.DAYS), ADMIN_ID);
        eventService.updateStatus(event.getId(), EventStatus.CFP_OPEN);

        // --- CAMBIO AQUÍ: Creamos usuarios de verdad en la base de datos ---
        User userA = userRegistrationService.registerSpeaker(
                "speaker.a." + UUID.randomUUID() + "@test.com", "Speaker A", "pwd123");
        User userB = userRegistrationService.registerSpeaker(
                "speaker.b." + UUID.randomUUID() + "@test.com", "Speaker B", "pwd123");

        speakerA = userA.getId();
        speakerB = userB.getId();
        // ------------------------------------------------------------------

        accepted1 = createAcceptedSubmission(speakerA, "Charla A");
        accepted2 = createAcceptedSubmission(speakerB, "Charla B");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void should_create_slot_for_accepted_submission() {
        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);
        AgendaSlot slot = agendaService.saveSlot(
                event.getId(),
                accepted1.getId(),
                event.getTracks().get(0).getId(),
                start,
                start.plus(30, ChronoUnit.MINUTES),
                null);

        assertThat(slot.getId()).isNotNull();
        assertThat(slot.getSubmission().getId()).isEqualTo(accepted1.getId());
    }

    @Test
    void should_reject_slot_for_non_accepted_submission() {
        SubmissionData data = new SubmissionData();
        data.setTitle("No aceptada");
        data.setAbstractText("Una propuesta cualquiera con texto suficientemente largo.");
        data.setLevel(SubmissionLevel.INTERMEDIATE);

        // --- CAMBIO AQUÍ: Usamos un speaker real (speakerA) en lugar de un UUID random ---
        Submission draft = submissionService.createDraft(event.getId(), speakerA, data);

        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);
        assertThatThrownBy(() -> agendaService.saveSlot(
                        event.getId(),
                        draft.getId(),
                        event.getTracks().get(0).getId(),
                        start,
                        start.plus(30, ChronoUnit.MINUTES),
                        null))
                .isInstanceOf(AgendaConflictException.class)
                .hasMessageContaining("ACCEPTED");
    }

    @Test
    void should_reject_overlapping_slots_in_same_track() {
        UUID trackId = event.getTracks().get(0).getId();
        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);

        agendaService.saveSlot(
                event.getId(), accepted1.getId(), trackId, start, start.plus(30, ChronoUnit.MINUTES), null);

        assertThatThrownBy(() -> agendaService.saveSlot(
                        event.getId(),
                        accepted2.getId(),
                        trackId,
                        start.plus(10, ChronoUnit.MINUTES),
                        start.plus(40, ChronoUnit.MINUTES),
                        null))
                .isInstanceOf(AgendaConflictException.class)
                .hasMessageContaining("track");
    }

    @Test
    void should_reject_same_speaker_in_simultaneous_slots() {
        Submission accepted3 = createAcceptedSubmission(speakerA, "Otra de A"); // mismo speaker
        UUID trackA = event.getTracks().get(0).getId();
        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);

        agendaService.saveSlot(
                event.getId(), accepted1.getId(), trackA, start, start.plus(30, ChronoUnit.MINUTES), null);

        assertThatThrownBy(() -> agendaService.saveSlot(
                        event.getId(), accepted3.getId(), trackA, start, start.plus(30, ChronoUnit.MINUTES), null))
                .isInstanceOf(AgendaConflictException.class);
    }

    @Test
    void should_allow_consecutive_slots_in_same_track() {
        UUID trackId = event.getTracks().get(0).getId();
        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);

        agendaService.saveSlot(
                event.getId(), accepted1.getId(), trackId, start, start.plus(30, ChronoUnit.MINUTES), null);

        AgendaSlot next = agendaService.saveSlot(
                event.getId(),
                accepted2.getId(),
                trackId,
                start.plus(31, ChronoUnit.MINUTES),
                start.plus(60, ChronoUnit.MINUTES),
                null);

        assertThat(next.getId()).isNotNull();
    }

    @Test
    void should_publish_event_when_all_accepted_have_slots() {
        UUID trackId = event.getTracks().get(0).getId();
        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);

        agendaService.saveSlot(
                event.getId(), accepted1.getId(), trackId, start, start.plus(30, ChronoUnit.MINUTES), null);
        agendaService.saveSlot(
                event.getId(),
                accepted2.getId(),
                trackId,
                start.plus(31, ChronoUnit.MINUTES),
                start.plus(60, ChronoUnit.MINUTES),
                null);

        eventService.updateStatus(event.getId(), EventStatus.CFP_CLOSED);
        eventService.updateStatus(event.getId(), EventStatus.REVIEW);

        agendaService.publishEvent(event.getId());

        Event reloaded = eventService.findById(event.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void should_reject_publish_when_accepted_submissions_missing_slots() {
        UUID trackId = event.getTracks().get(0).getId();
        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);

        // Solo asigna 1 de 2 aceptadas
        agendaService.saveSlot(
                event.getId(), accepted1.getId(), trackId, start, start.plus(30, ChronoUnit.MINUTES), null);

        eventService.updateStatus(event.getId(), EventStatus.CFP_CLOSED);
        eventService.updateStatus(event.getId(), EventStatus.REVIEW);

        assertThatThrownBy(() -> agendaService.publishEvent(event.getId()))
                .isInstanceOf(AgendaIncompleteException.class)
                .hasMessageContaining("Charla B");
    }

    @Test
    void should_create_slot_without_track() {
        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);
        AgendaSlot slot = agendaService.saveSlot(
                event.getId(), accepted1.getId(), null, start, start.plus(30, ChronoUnit.MINUTES), null);

        assertThat(slot.getId()).isNotNull();
        assertThat(slot.getTrack()).isNull();
    }

    @Test
    void should_create_slot_without_submission() {
        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);
        AgendaSlot slot = agendaService.saveSlot(
                event.getId(),
                null,
                event.getTracks().get(0).getId(),
                start,
                start.plus(30, ChronoUnit.MINUTES),
                "Break");

        assertThat(slot.getId()).isNotNull();
        assertThat(slot.getSubmission()).isNull();
        assertThat(slot.getTitleOverride()).isEqualTo("Break");
    }

    @Test
    void should_reject_slot_with_invalid_time_range() {
        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);
        assertThatThrownBy(() -> agendaService.saveSlot(
                        event.getId(),
                        accepted1.getId(),
                        event.getTracks().get(0).getId(),
                        start.plus(30, ChronoUnit.MINUTES),
                        start,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startsAt debe ser antes de endsAt");
    }

    @Test
    void should_move_slot_to_different_track() {
        UUID trackA = event.getTracks().get(0).getId();
        UUID trackB = event.getTracks().get(0).getId();
        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);

        AgendaSlot slot = agendaService.saveSlot(
                event.getId(), accepted1.getId(), trackA, start, start.plus(30, ChronoUnit.MINUTES), null);

        AgendaSlot moved = agendaService.moveSlot(
                slot.getId(), trackB, start.plus(60, ChronoUnit.MINUTES), start.plus(90, ChronoUnit.MINUTES));

        assertThat(moved.getId()).isEqualTo(slot.getId());
    }

    @Test
    void should_delete_slot() {
        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);
        AgendaSlot slot = agendaService.saveSlot(
                event.getId(),
                accepted1.getId(),
                event.getTracks().get(0).getId(),
                start,
                start.plus(30, ChronoUnit.MINUTES),
                null);

        agendaService.deleteSlot(slot.getId());

        assertThat(agendaService.findById(slot.getId())).isEmpty();
    }

    @Test
    void should_find_slot_by_id() {
        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);
        AgendaSlot slot = agendaService.saveSlot(
                event.getId(),
                accepted1.getId(),
                event.getTracks().get(0).getId(),
                start,
                start.plus(30, ChronoUnit.MINUTES),
                null);

        assertThat(agendaService.findById(slot.getId())).isPresent();
    }

    @Test
    void should_reject_publish_when_not_in_review_status() {
        assertThatThrownBy(() -> agendaService.publishEvent(event.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REVIEW");
    }

    @Test
    void should_generate_valid_ical() {
        UUID trackId = event.getTracks().get(0).getId();
        Instant start = Instant.parse("2026-05-15T19:00:00Z");

        agendaService.saveSlot(
                event.getId(), accepted1.getId(), trackId, start, start.plus(30, ChronoUnit.MINUTES), null);

        var slots = agendaService.listForEvent(event.getId());
        String ical = ICalGenerator.generate(event, slots);

        assertThat(ical).contains("BEGIN:VCALENDAR");
        assertThat(ical).contains("END:VCALENDAR");
        assertThat(ical).contains("BEGIN:VEVENT");
        assertThat(ical).contains("DTSTART:20260515T190000Z");
        assertThat(ical).contains("SUMMARY:Charla A");
    }

    @Test
    void should_generate_ical_without_location() {
        UUID trackId = event.getTracks().get(0).getId();
        Instant start = Instant.parse("2026-05-15T19:00:00Z");

        agendaService.saveSlot(
                event.getId(), accepted1.getId(), trackId, start, start.plus(30, ChronoUnit.MINUTES), null);

        var slots = agendaService.listForEvent(event.getId());
        String ical = ICalGenerator.generate(event, slots);

        assertThat(ical).contains("SUMMARY:Charla A");
        assertThat(ical).doesNotContain("LOCATION:");
    }

    @Test
    void should_generate_ical_for_slot_without_track() {
        Instant start = Instant.parse("2026-05-15T19:00:00Z");

        agendaService.saveSlot(
                event.getId(), accepted1.getId(), null, start, start.plus(30, ChronoUnit.MINUTES), "Break");

        var slots = agendaService.listForEvent(event.getId());
        String ical = ICalGenerator.generate(event, slots);

        assertThat(ical).contains("SUMMARY:Break");
        assertThat(ical).doesNotContain("CATEGORIES:");
    }

    private Submission createAcceptedSubmission(UUID speakerId, String title) {
        SubmissionData data = new SubmissionData();
        data.setTitle(title);
        data.setAbstractText("Abstract con suficiente longitud para validaciones del binder.");
        data.setLevel(SubmissionLevel.INTERMEDIATE);

        User reviewer = userRegistrationService.registerSpeaker(
                "reviewer." + UUID.randomUUID() + "@test.com", "Reviewer Test", "pwd123");

        Submission s = submissionService.createAndSubmit(event.getId(), speakerId, data);

        // Usamos el ID del reviewer real creado arriba
        reviewService.submitOrUpdateScore(s.getId(), reviewer.getId(), 5, "ok");

        reviewService.accept(s.getId());
        return submissionService.findById(s.getId()).orElseThrow();
    }
}
