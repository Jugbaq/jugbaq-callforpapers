package com.jugbaq.cfp.publishing;

import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventRepository;
import com.jugbaq.cfp.events.domain.EventStatus;
import com.jugbaq.cfp.events.domain.EventTrack;
import com.jugbaq.cfp.publishing.domain.AgendaConflictException;
import com.jugbaq.cfp.publishing.domain.AgendaIncompleteException;
import com.jugbaq.cfp.publishing.domain.AgendaSlot;
import com.jugbaq.cfp.publishing.domain.AgendaSlotRepository;
import com.jugbaq.cfp.submissions.domain.Submission;
import com.jugbaq.cfp.submissions.domain.SubmissionRepository;
import com.jugbaq.cfp.submissions.domain.SubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AgendaService {

    private final AgendaSlotRepository slotRepository;
    private final EventRepository eventRepository;
    private final SubmissionRepository submissionRepository;

    public AgendaService(AgendaSlotRepository slotRepository,
                         EventRepository eventRepository,
                         SubmissionRepository submissionRepository) {
        this.slotRepository = slotRepository;
        this.eventRepository = eventRepository;
        this.submissionRepository = submissionRepository;
    }

    /**
     * Crea o actualiza un slot. Valida conflictos antes de guardar.
     * Si submission != null, debe estar en estado ACCEPTED o CONFIRMED.
     */
    public AgendaSlot saveSlot(UUID eventId, UUID submissionId, UUID trackId,
                               Instant startsAt, Instant endsAt, String titleOverride) {
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("startsAt debe ser antes de endsAt");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        Submission submission = null;
        if (submissionId != null) {
            submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new IllegalArgumentException("Submission no encontrada"));
            if (submission.getStatus() != SubmissionStatus.ACCEPTED
                    && submission.getStatus() != SubmissionStatus.CONFIRMED) {
                throw new AgendaConflictException(
                        "Solo submissions ACCEPTED o CONFIRMED pueden ir en la agenda");
            }
        }

        EventTrack track = null;
        if (trackId != null) {
            track = event.getTracks().stream()
                    .filter(t -> t.getId().equals(trackId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Track no pertenece al evento"));
        }

        AgendaSlot newSlot = new AgendaSlot(event, submission, track, startsAt, endsAt);
        newSlot.setTitleOverride(titleOverride);

        validateNoConflicts(newSlot, null);

        return slotRepository.save(newSlot);
    }

    /**
     * Mueve un slot existente a otro track o tiempo. Re-valida conflictos.
     */
    public AgendaSlot moveSlot(UUID slotId, UUID newTrackId, Instant newStartsAt, Instant newEndsAt) {
        AgendaSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot no encontrado"));

        if (newTrackId != null) {
            EventTrack track = slot.getEvent().getTracks().stream()
                    .filter(t -> t.getId().equals(newTrackId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Track no pertenece al evento"));
            slot.setTrack(track);
        }
        slot.setStartsAt(newStartsAt);
        slot.setEndsAt(newEndsAt);

        validateNoConflicts(slot, slotId);
        return slotRepository.save(slot);
    }

    public void deleteSlot(UUID slotId) {
        slotRepository.deleteById(slotId);
    }

    /**
     * Publica el evento. Valida que toda submission ACCEPTED tenga slot asignado.
     */
    public void publishEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        if (event.getStatus() != EventStatus.REVIEW) {
            throw new IllegalStateException(
                    "El evento debe estar en REVIEW para publicar. Estado actual: " + event.getStatus());
        }

        List<Submission> accepted = submissionRepository.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream()
                .filter(s -> s.getStatus() == SubmissionStatus.ACCEPTED
                        || s.getStatus() == SubmissionStatus.CONFIRMED)
                .toList();

        Set<UUID> slottedSubmissionIds = new HashSet<>();
        slotRepository.findByEventOrdered(eventId).stream()
                .filter(s -> s.getSubmission() != null)
                .forEach(s -> slottedSubmissionIds.add(s.getSubmission().getId()));

        List<String> missing = new ArrayList<>();
        for (Submission s : accepted) {
            if (!slottedSubmissionIds.contains(s.getId())) {
                missing.add(s.getTitle());
            }
        }
        if (!missing.isEmpty()) {
            throw new AgendaIncompleteException(
                    "Faltan asignar slots para: " + String.join(", ", missing));
        }

        event.transitionTo(EventStatus.PUBLISHED);
        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AgendaSlot> listForEvent(UUID eventId) {
        return slotRepository.findByEventOrdered(eventId);
    }

    @Transactional(readOnly = true)
    public Optional<AgendaSlot> findById(UUID slotId) {
        return slotRepository.findById(slotId);
    }

    // --- Validaciones ---

    private void validateNoConflicts(AgendaSlot slot, UUID excludeSlotId) {
        List<AgendaSlot> existing = slotRepository.findByEventOrdered(slot.getEvent().getId());

        for (AgendaSlot other : existing) {
            if (excludeSlotId != null && other.getId().equals(excludeSlotId)) continue;
            if (!slot.overlapsWith(other)) continue;

            // Conflicto 1: mismo track al mismo tiempo
            if (slot.getTrack() != null && other.getTrack() != null
                    && slot.getTrack().getId().equals(other.getTrack().getId())) {
                throw new AgendaConflictException(
                        "Conflicto de track: ya hay un slot en '" + other.getTrack().getName()
                                + "' que se solapa con este horario");
            }

            // Conflicto 2: mismo speaker en dos lugares al mismo tiempo
            if (slot.getSubmission() != null && other.getSubmission() != null) {
                UUID newSpeaker = slot.getSubmission().getSpeakerId();
                UUID otherSpeaker = other.getSubmission().getSpeakerId();
                if (newSpeaker.equals(otherSpeaker)) {
                    throw new AgendaConflictException(
                            "El speaker ya tiene una charla simultánea: '"
                                    + other.getSubmission().getTitle() + "'");
                }
            }
        }
    }
}