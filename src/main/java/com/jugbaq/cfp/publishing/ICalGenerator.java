package com.jugbaq.cfp.publishing;

import com.jugbaq.cfp.events.domain.Event;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ICalGenerator {

    private static final DateTimeFormatter ICAL_DT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private ICalGenerator() {}

    public static String generate(Event event, List<AgendaSlotSummary> slots) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//JUGBAQ//CallForPapers//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        sb.append("X-WR-CALNAME:").append(escape(event.getName())).append("\r\n");

        for (AgendaSlotSummary slot : slots) {
            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:").append(slot.id()).append("@jugbaq.dev\r\n");
            sb.append("DTSTAMP:").append(ICAL_DT.format(Instant.now())).append("\r\n");
            sb.append("DTSTART:").append(ICAL_DT.format(slot.startsAt())).append("\r\n");
            sb.append("DTEND:").append(ICAL_DT.format(slot.endsAt())).append("\r\n");
            sb.append("SUMMARY:").append(escape(slot.displayTitle())).append("\r\n");

            if (slot.abstractText() != null && !slot.abstractText().isBlank()) {
                sb.append("DESCRIPTION:").append(escape(slot.abstractText())).append("\r\n");
            }

            if (slot.trackName() != null && !slot.trackName().isBlank()) {
                sb.append("CATEGORIES:").append(escape(slot.trackName())).append("\r\n");
            }

            if (event.getLocation() != null && !event.getLocation().isBlank()) {
                sb.append("LOCATION:").append(escape(event.getLocation())).append("\r\n");
            }
            sb.append("END:VEVENT\r\n");
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n");
    }
}
