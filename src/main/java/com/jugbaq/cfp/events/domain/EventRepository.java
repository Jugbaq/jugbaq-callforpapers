package com.jugbaq.cfp.events.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, UUID> {
    Optional<Event> findBySlug(String slug);

    List<Event> findByStatusOrderByEventDateDesc(EventStatus status);

    List<Event> findAllByOrderByEventDateDesc();

    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.tracks WHERE e.slug = :slug")
    Optional<Event> findBySlugWithTracks(@Param("slug") String slug);
}
