package com.jugbaq.cfp.events;

import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventStatus;
import java.time.Instant;
import java.util.UUID;

public record EventSummary(UUID id, String name, String slug, Instant eventDate, EventStatus status) {
    public static EventSummary from(Event e) {
        return new EventSummary(e.getId(), e.getName(), e.getSlug(), e.getEventDate(), e.getStatus());
    }
}
