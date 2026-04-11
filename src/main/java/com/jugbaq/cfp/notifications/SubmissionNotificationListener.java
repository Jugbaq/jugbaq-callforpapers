package com.jugbaq.cfp.notifications;

import com.jugbaq.cfp.submissions.events.SubmissionSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener placeholder. 
 * - Email al speaker confirmando recepción (vía Mailhog en dev)
 * - Email a los organizadores notificando nueva submission
 * - Registro en tabla 'notifications' para badge in-app
 */
@Component
public class SubmissionNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(SubmissionNotificationListener.class);

    @Async
    @EventListener
    public void onSubmissionSubmitted(SubmissionSubmittedEvent event) {
        log.info(
                "📬 Nueva propuesta recibida: '{}' (id={}) del speaker {} para el evento {} en tenant {}",
                event.title(),
                event.submissionId(),
                event.speakerId(),
                event.eventId(),
                event.tenantId()
        );

    }
}
