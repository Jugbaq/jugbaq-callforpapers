package com.jugbaq.cfp.publishing;

import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CalendarController {

    private final EventRepository eventRepository;
    private final AgendaService agendaService;

    public CalendarController(EventRepository eventRepository, AgendaService agendaService) {
        this.eventRepository = eventRepository;
        this.agendaService = agendaService;
    }

    @GetMapping("/t/{tenantSlug}/api/events/{eventSlug}/calendar.ics")
    public ResponseEntity<String> downloadIcal(@PathVariable String tenantSlug, @PathVariable String eventSlug) {
        // El TenantFilter ya seteó el contexto al ver /t/{tenantSlug}/...
        Event event = eventRepository
                .findBySlug(eventSlug)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        var slots = agendaService.listForEvent(event.getId());
        String ical = ICalGenerator.generate(event, slots);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + eventSlug + ".ics\"")
                .body(ical);
    }
}
