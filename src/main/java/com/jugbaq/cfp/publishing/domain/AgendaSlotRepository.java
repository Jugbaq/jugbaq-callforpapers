package com.jugbaq.cfp.publishing.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendaSlotRepository extends JpaRepository<AgendaSlot, UUID> {

    @Query("SELECT s FROM AgendaSlot s WHERE s.event.id = :eventId ORDER BY s.startsAt ASC")
    List<AgendaSlot> findByEventOrdered(UUID eventId);

    void deleteByEventId(UUID eventId);

    @Query(
            "SELECT s FROM AgendaSlot s LEFT JOIN FETCH s.submission LEFT JOIN FETCH s.track WHERE s.event.id = :eventId ORDER BY s.startsAt ASC")
    List<AgendaSlot> findAllByEventIdWithDetails(@Param("eventId") UUID eventId);
}
