package com.jugbaq.cfp.submissions.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByEventIdAndSpeakerId(UUID eventId, UUID speakerId);

    long countByEventIdAndSpeakerIdAndStatusNot(
            UUID eventId, UUID speakerId, SubmissionStatus excludedStatus);

    @Query("SELECT s FROM Submission s " +
            "JOIN FETCH s.event " +
            "LEFT JOIN FETCH s.format " +
            "LEFT JOIN FETCH s.track " +
            "WHERE s.speakerId = :speakerId " +
            "ORDER BY s.createdAt DESC")
    List<Submission> findBySpeakerIdOrderByCreatedAtDesc(@Param("speakerId") UUID speakerId);
}
