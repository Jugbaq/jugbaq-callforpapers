package com.jugbaq.cfp.submissions;

import com.jugbaq.cfp.submissions.domain.Submission;
import com.jugbaq.cfp.submissions.domain.SubmissionLevel;
import com.jugbaq.cfp.submissions.domain.SubmissionStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SubmissionSummary(
        UUID id,
        String title,
        String eventName,
        UUID speakerId,
        SubmissionStatus status,
        SubmissionLevel level,
        String formatName,
        String abstractText,
        String pitch,
        Set<String> tags,
        Instant submittedAt) {
    public static SubmissionSummary from(Submission s) {
        return new SubmissionSummary(
                s.getId(),
                s.getTitle(),
                s.getEvent().getName(),
                s.getSpeakerId(),
                s.getStatus(),
                s.getLevel(),
                s.getFormat() != null ? s.getFormat().getName() : null,
                s.getAbstractText(),
                s.getPitch(),
                Set.copyOf(s.getTags()),
                s.getSubmittedAt());
    }
}
