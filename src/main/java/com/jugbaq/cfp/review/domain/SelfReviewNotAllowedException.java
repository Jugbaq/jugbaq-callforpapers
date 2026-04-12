package com.jugbaq.cfp.review.domain;

public class SelfReviewNotAllowedException extends RuntimeException {
    public SelfReviewNotAllowedException() {
        super("Un reviewer no puede revisar su propia propuesta");
    }
}
