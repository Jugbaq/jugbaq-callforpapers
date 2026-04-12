package com.jugbaq.cfp.submissions.events;

import java.time.Instant;
import java.util.UUID;

public record SubmissionSubmittedEvent(
        UUID submissionId, UUID eventId, UUID speakerId, UUID tenantId, String title, Instant submittedAt) {}
