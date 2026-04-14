package com.jugbaq.cfp.publishing;

import com.jugbaq.cfp.publishing.domain.AgendaSlot;
import java.time.Instant;
import java.util.UUID;

public record AgendaSlotSummary(
        UUID id,
        UUID eventId,
        Instant startsAt,
        Instant endsAt,
        String displayTitle,
        UUID trackId,
        String trackName,
        UUID submissionId,
        UUID speakerId,
        String abstractText) {
    public static AgendaSlotSummary from(AgendaSlot slot) {
        UUID trackId = slot.getTrack() != null ? slot.getTrack().getId() : null;
        String trackName = slot.getTrack() != null ? slot.getTrack().getName() : null;

        UUID submissionId = slot.getSubmission() != null ? slot.getSubmission().getId() : null;
        UUID speakerId = slot.getSubmission() != null ? slot.getSubmission().getSpeakerId() : null;
        String abstractText =
                slot.getSubmission() != null ? slot.getSubmission().getAbstractText() : null;

        return new AgendaSlotSummary(
                slot.getId(),
                slot.getEvent().getId(),
                slot.getStartsAt(),
                slot.getEndsAt(),
                slot.displayTitle(),
                trackId,
                trackName,
                submissionId,
                speakerId,
                abstractText);
    }
}
