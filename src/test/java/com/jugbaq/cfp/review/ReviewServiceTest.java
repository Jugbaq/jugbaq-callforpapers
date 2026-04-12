package com.jugbaq.cfp.review;

import com.jugbaq.cfp.TestcontainersConfiguration;
import com.jugbaq.cfp.events.EventService;
import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventStatus;
import com.jugbaq.cfp.review.domain.Review;
import com.jugbaq.cfp.review.domain.SelfReviewNotAllowedException;
import com.jugbaq.cfp.shared.domain.TenantRepository;
import com.jugbaq.cfp.shared.tenant.TenantContext;
import com.jugbaq.cfp.submissions.SubmissionData;
import com.jugbaq.cfp.submissions.SubmissionService;
import com.jugbaq.cfp.submissions.domain.Submission;
import com.jugbaq.cfp.submissions.domain.SubmissionLevel;
import com.jugbaq.cfp.submissions.domain.SubmissionStatus;
import com.jugbaq.cfp.submissions.events.SubmissionAcceptedEvent;
import com.jugbaq.cfp.submissions.events.SubmissionRejectedEvent;

import com.jugbaq.cfp.users.UserRegistrationService;
import com.jugbaq.cfp.users.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@RecordApplicationEvents
@Transactional
class ReviewServiceTest {

    @Autowired ReviewService reviewService;
    @Autowired SubmissionService submissionService;
    @Autowired EventService eventService;
    @Autowired TenantRepository tenantRepository;
    @Autowired ApplicationEvents applicationEvents;

    private static final UUID ADMIN_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private UUID speakerId;
    private UUID reviewerId;
    private Submission submission;

    @Autowired
    UserRegistrationService userRegistrationService;

    @BeforeEach
    void setUp() {
        var tenant = tenantRepository.findBySlug("jugbaq").orElseThrow();
        TenantContext.set(tenant.getId(), "jugbaq");

        User speaker = userRegistrationService.registerSpeaker(
                "speaker.review." + UUID.randomUUID() + "@test.com", "Speaker Review", "pwd123");
        User reviewer = userRegistrationService.registerSpeaker(
                "reviewer." + UUID.randomUUID() + "@test.com", "Reviewer Test", "pwd123");
        speakerId = speaker.getId();
        reviewerId = reviewer.getId();

        Event event = eventService.createEvent(
                "review-test-" + UUID.randomUUID(), "Review Test",
                Instant.now().plusSeconds(86400 * 30), ADMIN_ID);
        eventService.updateStatus(event.getId(), EventStatus.CFP_OPEN);

        SubmissionData data = new SubmissionData();
        data.setTitle("Charla a revisar");
        data.setAbstractText("Abstract con suficiente longitud para validaciones del binder.");
        data.setLevel(SubmissionLevel.INTERMEDIATE);
        submission = submissionService.createAndSubmit(event.getId(), speakerId, data);
    }

    @AfterEach
    void cleanup() { TenantContext.clear(); }

    @Test
    void should_submit_score_and_transition_to_under_review() {
        Review review = reviewService.submitOrUpdateScore(
                submission.getId(), reviewerId, 4, "Buen tema, abstract claro");

        assertThat(review.getId()).isNotNull();
        assertThat(review.getScore()).isEqualTo(4);

        Submission reloaded = submissionService.findById(submission.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubmissionStatus.UNDER_REVIEW);
    }

    @Test
    void should_reject_self_review() {
        assertThatThrownBy(() ->
                reviewService.submitOrUpdateScore(submission.getId(), speakerId, 5, "Me gusta mucho")
        ).isInstanceOf(SelfReviewNotAllowedException.class);
    }

    @Test
    void should_update_existing_review_for_same_reviewer() {
        reviewService.submitOrUpdateScore(submission.getId(), reviewerId, 3, "Inicial");
        Review updated = reviewService.submitOrUpdateScore(
                submission.getId(), reviewerId, 5, "Cambié de opinión");

        assertThat(updated.getScore()).isEqualTo(5);
        assertThat(reviewService.listReviewsForSubmission(submission.getId())).hasSize(1);
    }

    @Test
    void should_calculate_average_score() {
        User rev1 = userRegistrationService.registerSpeaker("rev1." + UUID.randomUUID() + "@test.com", "Rev 1", "pwd");
        User rev2 = userRegistrationService.registerSpeaker("rev2." + UUID.randomUUID() + "@test.com", "Rev 2", "pwd");
        User rev3 = userRegistrationService.registerSpeaker("rev3." + UUID.randomUUID() + "@test.com", "Rev 3", "pwd");

        reviewService.submitOrUpdateScore(submission.getId(), rev1.getId(), 4, null);
        reviewService.submitOrUpdateScore(submission.getId(), rev2.getId(), 5, null);
        reviewService.submitOrUpdateScore(submission.getId(), rev3.getId(), 3, null);

        assertThat(reviewService.averageScore(submission.getId())).hasValue(4.0);
    }

    @Test
    void should_accept_submission_and_publish_event() {
        reviewService.submitOrUpdateScore(submission.getId(), reviewerId, 5, "Excelente");
        reviewService.accept(submission.getId());

        Submission reloaded = submissionService.findById(submission.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubmissionStatus.ACCEPTED);

        long events = applicationEvents.stream(SubmissionAcceptedEvent.class)
                .filter(e -> e.submissionId().equals(submission.getId()))
                .count();
        assertThat(events).isEqualTo(1);
    }

    @Test
    void should_reject_submission_with_feedback() {
        reviewService.submitOrUpdateScore(submission.getId(), reviewerId, 2, "Tema repetido");
        reviewService.reject(submission.getId(), "Ya tenemos charlas similares");

        Submission reloaded = submissionService.findById(submission.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubmissionStatus.REJECTED);

        var rejectedEvent = applicationEvents.stream(SubmissionRejectedEvent.class)
                .filter(e -> e.submissionId().equals(submission.getId()))
                .findFirst().orElseThrow();
        assertThat(rejectedEvent.feedback()).contains("similares");
    }

    @Test
    void should_post_discussion_message() {
        var msg = reviewService.postDiscussion(
                submission.getId(), reviewerId, "Me parece interesante pero el abstract es vago");

        assertThat(msg.getId()).isNotNull();
        assertThat(reviewService.listDiscussion(submission.getId())).hasSize(1);
    }
}
