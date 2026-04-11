package com.jugbaq.cfp.submissions.domain;

public class SubmissionLimitExceededException extends RuntimeException {
    public SubmissionLimitExceededException(int max) {
        super("Ya alcanzaste el límite de " + max + " propuestas para este evento");
    }
}
