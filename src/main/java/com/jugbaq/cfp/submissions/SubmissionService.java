package com.jugbaq.cfp.submissions;

import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventRepository;
import com.jugbaq.cfp.shared.tenant.TenantContext;
import com.jugbaq.cfp.submissions.domain.CfpClosedException;
import com.jugbaq.cfp.submissions.domain.Submission;
import com.jugbaq.cfp.submissions.domain.SubmissionLimitExceededException;
import com.jugbaq.cfp.submissions.domain.SubmissionRepository;
import com.jugbaq.cfp.submissions.domain.SubmissionStatus;
import com.jugbaq.cfp.submissions.events.SubmissionSubmittedEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubmissionService {

    private final SubmissionRepository repository;
    private final EventRepository eventRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SubmissionService(
            SubmissionRepository repository,
            EventRepository eventRepository,
            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Crea una propuesta en estado DRAFT. No dispara evento aún.
     * Valida que el CFP esté abierto y que el speaker no haya excedido el límite.
     */
    public Submission createDraft(UUID eventId, UUID speakerId, SubmissionData data) {
        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        if (!event.isCfpOpen()) {
            throw new CfpClosedException(event.getName());
        }

        long activeCount =
                repository.countByEventIdAndSpeakerIdAndStatusNot(eventId, speakerId, SubmissionStatus.WITHDRAWN);

        if (activeCount >= event.getMaxSubmissionsPerSpeaker()) {
            throw new SubmissionLimitExceededException(event.getMaxSubmissionsPerSpeaker());
        }

        UUID tenantId = TenantContext.requireTenantId();
        Submission submission = new Submission(tenantId, event, speakerId, data.getTitle(), data.getAbstractText());
        submission.updateContent(
                data.getTitle(), data.getAbstractText(), data.getPitch(), data.getLevel(), data.getTags());

        applyFormatAndTrack(submission, event, data);
        return repository.save(submission);
    }

    /**
     * Marca como SUBMITTED y publica el evento de dominio.
     * Aquí es donde notifications (Guía 05) escucha.
     */
    public Submission markAsSubmitted(UUID submissionId, UUID speakerId) {
        Submission submission = loadOwned(submissionId, speakerId);
        submission.transitionTo(SubmissionStatus.SUBMITTED);
        repository.save(submission);

        eventPublisher.publishEvent(new SubmissionSubmittedEvent(
                submission.getId(),
                submission.getEvent().getId(),
                submission.getSpeakerId(),
                submission.getTenantId(),
                submission.getTitle(),
                submission.getSubmittedAt()));

        return submission;
    }

    /**
     * Atajo: crea y envía en una sola operación (para el botón "Enviar propuesta").
     */
    public Submission createAndSubmit(UUID eventId, UUID speakerId, SubmissionData data) {
        Submission draft = createDraft(eventId, speakerId, data);
        return markAsSubmitted(draft.getId(), speakerId);
    }

    public Submission updateDraft(UUID submissionId, UUID speakerId, SubmissionData data) {
        Submission submission = loadOwned(submissionId, speakerId);
        submission.updateContent(
                data.getTitle(), data.getAbstractText(), data.getPitch(), data.getLevel(), data.getTags());
        applyFormatAndTrack(submission, submission.getEvent(), data);
        return repository.save(submission);
    }

    public Submission withdraw(UUID submissionId, UUID speakerId) {
        Submission submission = loadOwned(submissionId, speakerId);
        submission.transitionTo(SubmissionStatus.WITHDRAWN);
        return repository.save(submission);
    }

    @Transactional(readOnly = true)
    public List<Submission> listBySpeaker(UUID speakerId) {
        return repository.findBySpeakerIdOrderByCreatedAtDesc(speakerId);
    }

    @Transactional(readOnly = true)
    public Optional<Submission> findById(UUID id) {
        return repository.findById(id);
    }

    // --- Privados ---

    private Submission loadOwned(UUID submissionId, UUID speakerId) {
        Submission submission = repository
                .findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Propuesta no encontrada"));
        submission.assertOwnedBy(speakerId);
        return submission;
    }

    private void applyFormatAndTrack(Submission submission, Event event, SubmissionData data) {
        if (data.getFormatId() != null) {
            event.getFormats().stream()
                    .filter(f -> f.getId().equals(data.getFormatId()))
                    .findFirst()
                    .ifPresent(submission::setFormat);
        }
        if (data.getTrackId() != null) {
            event.getTracks().stream()
                    .filter(t -> t.getId().equals(data.getTrackId()))
                    .findFirst()
                    .ifPresent(submission::setTrack);
        }
    }

    @Transactional(readOnly = true)
    public long countActiveSubmissionsBySpeaker(UUID eventId, UUID speakerId) {
        return repository.countByEventIdAndSpeakerIdAndStatusNot(eventId, speakerId, SubmissionStatus.WITHDRAWN);
    }

    @Transactional(readOnly = true)
    public List<Submission> listForReview(UUID eventId, SubmissionStatus statusFilter) {
        if (eventId != null && statusFilter != null) {
            return repository.findByEventIdAndStatusOrderByCreatedAtDesc(eventId, statusFilter);
        }
        if (eventId != null) {
            return repository.findByEventIdOrderByCreatedAtDesc(eventId);
        }

        return repository.findByStatusInOrderByCreatedAtDesc(List.of(
                SubmissionStatus.SUBMITTED,
                SubmissionStatus.UNDER_REVIEW,
                SubmissionStatus.ACCEPTED,
                SubmissionStatus.REJECTED));
    }
}
