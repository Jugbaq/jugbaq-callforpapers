package com.jugbaq.cfp.publishing.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AgendaSlotRepository extends JpaRepository<AgendaSlot, UUID> {

    @Query("SELECT s FROM AgendaSlot s WHERE s.event.id = :eventId ORDER BY s.startsAt ASC")
    List<AgendaSlot> findByEventOrdered(UUID eventId);

    @Query("SELECT s FROM AgendaSlot s WHERE s.submission.id = :submissionId")
    Optional<AgendaSlot> findBySubmissionId(UUID submissionId);

    void deleteByEventId(UUID eventId);
}
