package com.jugbaq.cfp.review.domain;

import com.jugbaq.cfp.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "review_discussions")
public class ReviewDiscussion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    protected ReviewDiscussion() {}

    public ReviewDiscussion(UUID submissionId, UUID authorId, String message) {
        this.submissionId = submissionId;
        this.authorId = authorId;
        this.message = message;
    }

    public UUID getId() { return id; }
    public UUID getSubmissionId() { return submissionId; }
    public UUID getAuthorId() { return authorId; }
    public String getMessage() { return message; }
}
