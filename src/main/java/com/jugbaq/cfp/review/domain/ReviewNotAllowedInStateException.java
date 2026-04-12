package com.jugbaq.cfp.review.domain;

import com.jugbaq.cfp.submissions.domain.SubmissionStatus;

public class ReviewNotAllowedInStateException extends RuntimeException {
    public ReviewNotAllowedInStateException(SubmissionStatus status) {
        super("No se puede revisar una propuesta en estado " + status);
    }
}
