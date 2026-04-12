package com.jugbaq.cfp.review.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewDiscussionRepository extends JpaRepository<ReviewDiscussion, UUID> {
    List<ReviewDiscussion> findBySubmissionIdOrderByCreatedAtAsc(UUID submissionId);
}
