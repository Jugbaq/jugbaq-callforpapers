package com.jugbaq.cfp.submissions.domain;

public class NotSubmissionOwnerException extends RuntimeException {
    public NotSubmissionOwnerException() {
        super("No eres el dueño de esta propuesta");
    }
}
