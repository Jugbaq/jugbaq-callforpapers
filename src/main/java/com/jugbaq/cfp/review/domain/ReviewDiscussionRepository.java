package com.jugbaq.cfp.review.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewDiscussionRepository extends JpaRepository<ReviewDiscussion, UUID> {
    List<ReviewDiscussion> findBySubmissionIdOrderByCreatedAtAsc(UUID submissionId);
}
