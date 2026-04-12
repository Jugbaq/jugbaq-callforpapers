package com.jugbaq.cfp.review;

import com.jugbaq.cfp.review.domain.Review;
import java.util.UUID;

public record ReviewSummary(
        UUID id,
        UUID reviewerId,
        int score,
        String comment
) {
    public static ReviewSummary from(Review review) {
        return new ReviewSummary(review.getId(), review.getReviewerId(), review.getScore(), review.getComment());
    }
}