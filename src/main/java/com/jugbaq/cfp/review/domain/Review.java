package com.jugbaq.cfp.review.domain;

import com.jugbaq.cfp.shared.domain.BaseEntity;
import com.jugbaq.cfp.shared.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"submission_id", "reviewer_id"}))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Review extends BaseEntity implements TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Column(nullable = false)
    private short score;

    @Column(columnDefinition = "text")
    private String comment;

    protected Review() {}

    public Review(UUID tenantId, UUID submissionId, UUID reviewerId, int score, String comment) {
        this.tenantId = tenantId;
        this.submissionId = submissionId;
        this.reviewerId = reviewerId;
        setScore(score);
        this.comment = comment;
    }

    public void update(int score, String comment) {
        setScore(score);
        this.comment = comment;
    }

    private void setScore(int score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Score debe estar entre 1 y 5");
        }
        this.score = (short) score;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public int getScore() {
        return score;
    }

    public String getComment() {
        return comment;
    }
}
