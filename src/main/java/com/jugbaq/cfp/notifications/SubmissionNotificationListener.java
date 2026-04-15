package com.jugbaq.cfp.notifications;

import com.jugbaq.cfp.submissions.events.SubmissionAcceptedEvent;
import com.jugbaq.cfp.submissions.events.SubmissionRejectedEvent;
import com.jugbaq.cfp.submissions.events.SubmissionSubmittedEvent;
import com.jugbaq.cfp.users.SpeakerSummary;
import com.jugbaq.cfp.users.UserQueryService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener placeholder.
 * - Email al speaker confirmando recepción (vía Mailhog en dev)
 * - Email a los organizadores notificando nueva submission
 * - Registro en tabla 'notifications' para badge in-app
 */
@Component
public class SubmissionNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(SubmissionNotificationListener.class);

    private final UserQueryService userQueryService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final String baseUrl;

    public SubmissionNotificationListener(
            UserQueryService userQueryService,
            NotificationService notificationService,
            EmailService emailService,
            @Value("${cfp.base-url:http://localhost:8080}") String baseUrl) {
        this.userQueryService = userQueryService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.baseUrl = baseUrl;
    }

    @Async
    @EventListener
    @Transactional
    public void onSubmissionSubmitted(SubmissionSubmittedEvent event) {
        log.info("Procesando notificación para submission {}", event.submissionId());

        SpeakerSummary speaker = userQueryService.getSpeakerInfo(event.speakerId());
        if (speaker == null) {
            log.warn("Speaker {} no encontrado, saltando notificación", event.speakerId());
            return;
        }

        notifySpeaker(speaker, event);
        notifyOrganizers(speaker, event);
    }

    private void notifySpeaker(SpeakerSummary speaker, SubmissionSubmittedEvent event) {
        Map<String, Object> payload = Map.of(
                "submissionId", event.submissionId().toString(),
                "title", event.title());
        notificationService.create(speaker.id(), event.tenantId(), NotificationType.SUBMISSION_RECEIVED, payload);

        Map<String, String> vars = Map.of(
                "speakerName",
                speaker.fullName(),
                "title",
                event.title(),
                "eventName",
                "JUGBAQ Meetup",
                "baseUrl",
                baseUrl);
        String subject = emailService.render(EmailTemplates.SUBMISSION_RECEIVED_SUBJECT, vars);
        String body = emailService.render(EmailTemplates.SUBMISSION_RECEIVED_BODY, vars);
        emailService.sendHtml(speaker.email(), subject, body);
    }

    private void notifyOrganizers(SpeakerSummary speaker, SubmissionSubmittedEvent event) {
        List<SpeakerSummary> organizers = userQueryService.findOrganizersAndAdmins(event.tenantId());

        for (SpeakerSummary organizer : organizers) {
            Map<String, Object> payload = Map.of(
                    "submissionId", event.submissionId().toString(),
                    "title", event.title(),
                    "speakerName", speaker.fullName());
            notificationService.create(
                    organizer.id(), event.tenantId(), NotificationType.SUBMISSION_NEW_FOR_REVIEW, payload);

            Map<String, String> vars = Map.of(
                    "organizerName", organizer.fullName(),
                    "speakerName", speaker.fullName(),
                    "title", event.title(),
                    "eventName", "JUGBAQ Meetup",
                    "baseUrl", baseUrl);
            String subject = emailService.render(EmailTemplates.SUBMISSION_NEW_SUBJECT, vars);
            String body = emailService.render(EmailTemplates.SUBMISSION_NEW_BODY, vars);
            emailService.sendHtml(organizer.email(), subject, body);
        }
    }

    @Async
    @EventListener
    @Transactional
    public void onSubmissionAccepted(SubmissionAcceptedEvent event) {
        log.info("Procesando notificación para submission {}", event.submissionId());

        SpeakerSummary speaker = userQueryService.getSpeakerInfo(event.speakerId());
        if (speaker == null) return;

        notificationService.create(
                speaker.id(),
                event.tenantId(),
                NotificationType.SUBMISSION_ACCEPTED,
                Map.of("submissionId", event.submissionId().toString(), "title", event.title()));

        Map<String, String> vars = Map.of(
                "speakerName",
                speaker.fullName(),
                "title",
                event.title(),
                "eventName",
                "JUGBAQ Meetup",
                "baseUrl",
                baseUrl);
        emailService.sendHtml(
                speaker.email(),
                emailService.render(EmailTemplates.SUBMISSION_ACCEPTED_SUBJECT, vars),
                emailService.render(EmailTemplates.SUBMISSION_ACCEPTED_BODY, vars));
    }

    @Async
    @EventListener
    @Transactional
    public void onSubmissionRejected(SubmissionRejectedEvent event) {
        SpeakerSummary speaker = userQueryService.getSpeakerInfo(event.speakerId());
        if (speaker == null) return;

        notificationService.create(
                speaker.id(),
                event.tenantId(),
                NotificationType.SUBMISSION_REJECTED,
                Map.of("submissionId", event.submissionId().toString(), "title", event.title()));

        String feedbackBlock = (event.feedback() != null && !event.feedback().isBlank())
                ? "<div style='background:#f5f5f5;padding:16px;border-radius:6px;margin:16px 0;'>"
                        + "<strong>Feedback de los organizadores:</strong><br>"
                        + event.feedback() + "</div>"
                : "";

        Map<String, String> vars = Map.of(
                "speakerName",
                speaker.fullName(),
                "title",
                event.title(),
                "eventName",
                "JUGBAQ Meetup",
                "baseUrl",
                baseUrl,
                "feedbackBlock",
                feedbackBlock);
        emailService.sendHtml(
                speaker.email(),
                emailService.render(EmailTemplates.SUBMISSION_REJECTED_SUBJECT, vars),
                emailService.render(EmailTemplates.SUBMISSION_REJECTED_BODY, vars));
    }
}
