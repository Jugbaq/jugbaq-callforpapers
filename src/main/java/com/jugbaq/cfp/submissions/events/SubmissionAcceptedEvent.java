package com.jugbaq.cfp.review.events;

import java.util.UUID;

public record SubmissionAcceptedEvent(
        UUID submissionId,
        UUID eventId,
        UUID speakerId,
        UUID tenantId,
        String title
) {}
