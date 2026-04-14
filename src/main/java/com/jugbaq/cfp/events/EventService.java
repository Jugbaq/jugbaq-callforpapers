package com.jugbaq.cfp.events;

import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventRepository;
import com.jugbaq.cfp.events.domain.EventStatus;
import com.jugbaq.cfp.shared.tenant.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EventService {

    private final EventRepository repository;

    public EventService(EventRepository repository) {
        this.repository = repository;
    }

    public Event createEvent(String slug, String name, Instant date, UUID createdBy) {
        UUID tenantId = TenantContext.requireTenantId();
        Event event = new Event(tenantId, slug, name, date, createdBy);
        event.addFormat("Charla 30 min", 30);
        event.addFormat("Lightning 10 min", 10);
        event.addTrack("General", "Track general");
        return repository.save(event);
    }

    @Transactional(readOnly = true)
    public List<Event> listAll() {
        return repository.findAllByOrderByEventDateDesc();
    }

    @Transactional(readOnly = true)
    public List<Event> listCfpOpen() {
        return repository.findByStatusOrderByEventDateDesc(EventStatus.CFP_OPEN).stream()
                .filter(Event::isCfpOpen)
                .toList();
    }

    public Event updateStatus(UUID eventId, EventStatus newStatus) {
        Event event = repository.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Event not found"));
        event.transitionTo(newStatus);
        return event;
    }

    public Event save(Event event) {
        return repository.save(event);
    }

    @Transactional(readOnly = true)
    public Optional<Event> findById(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Event getEventWithTracksBySlug(String slug) {
        return repository.findBySlugWithTracks(slug).orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<Event> findBySlugWithDetails(String slug) {
        return repository.findBySlug(slug).map(event -> {
            event.getFormats().forEach(f -> {});
            event.getTracks().forEach(t -> {});
            return event;
        });
    }
}
