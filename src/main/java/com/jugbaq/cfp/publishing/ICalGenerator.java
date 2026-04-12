package com.jugbaq.cfp.publishing;

import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.publishing.domain.AgendaSlot;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ICalGenerator {

    private static final DateTimeFormatter ICAL_DT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private ICalGenerator() {}

    public static String generate(Event event, List<AgendaSlot> slots) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//JUGBAQ//CallForPapers//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        sb.append("X-WR-CALNAME:").append(escape(event.getName())).append("\r\n");

        for (AgendaSlot slot : slots) {
            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:").append(slot.getId()).append("@jugbaq.dev\r\n");
            sb.append("DTSTAMP:").append(ICAL_DT.format(Instant.now())).append("\r\n");
            sb.append("DTSTART:").append(ICAL_DT.format(slot.getStartsAt())).append("\r\n");
            sb.append("DTEND:").append(ICAL_DT.format(slot.getEndsAt())).append("\r\n");
            sb.append("SUMMARY:").append(escape(slot.displayTitle())).append("\r\n");

            if (slot.getSubmission() != null && slot.getSubmission().getAbstractText() != null) {
                sb.append("DESCRIPTION:")
                        .append(escape(slot.getSubmission().getAbstractText()))
                        .append("\r\n");
            }
            if (slot.getTrack() != null) {
                sb.append("CATEGORIES:")
                        .append(escape(slot.getTrack().getName()))
                        .append("\r\n");
            }
            if (event.getLocation() != null) {
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
