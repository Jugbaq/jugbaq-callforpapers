package com.jugbaq.cfp.review.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findBySubmissionId(UUID submissionId);

    Optional<Review> findBySubmissionIdAndReviewerId(UUID submissionId, UUID reviewerId);

    @Query("SELECT AVG(r.score) FROM Review r WHERE r.submissionId = :submissionId")
    Double averageScoreForSubmission(UUID submissionId);

    long countBySubmissionId(UUID submissionId);
}
