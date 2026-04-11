package com.jugbaq.cfp.events.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    Optional<Event> findBySlug(String slug);
    List<Event> findByStatusOrderByEventDateDesc(EventStatus status);
    List<Event> findAllByOrderByEventDateDesc();
}
