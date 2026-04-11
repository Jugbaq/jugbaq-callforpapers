package com.jugbaq.cfp.review.events;

import java.util.UUID;

public record SubmissionRejectedEvent(
        UUID submissionId,
        UUID eventId,
        UUID speakerId,
        UUID tenantId,
        String title,
        String feedback   // mensaje opcional para el speaker
) {}
