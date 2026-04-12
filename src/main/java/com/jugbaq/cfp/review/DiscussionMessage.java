package com.jugbaq.cfp.review;

import com.jugbaq.cfp.review.domain.ReviewDiscussion;
import java.util.UUID;

public record DiscussionMessage(
        UUID id,
        UUID authorId,
        String message
) {
    public static DiscussionMessage from(ReviewDiscussion discussion) {
        return new DiscussionMessage(discussion.getId(), discussion.getAuthorId(), discussion.getMessage());
    }
}