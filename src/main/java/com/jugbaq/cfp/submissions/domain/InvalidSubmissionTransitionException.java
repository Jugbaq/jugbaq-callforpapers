package com.jugbaq.cfp.submissions.domain;

public class InvalidSubmissionTransitionException extends RuntimeException {
    public InvalidSubmissionTransitionException(SubmissionStatus from, SubmissionStatus to) {
        super("Transición inválida: " + from + " → " + to);
    }
}

