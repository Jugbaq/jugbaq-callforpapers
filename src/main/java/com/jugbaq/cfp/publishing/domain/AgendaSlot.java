package com.jugbaq.cfp.publishing.domain;

import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventTrack;
import com.jugbaq.cfp.shared.domain.BaseEntity;
import com.jugbaq.cfp.submissions.domain.Submission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "agenda_slots")
public class AgendaSlot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private EventTrack track;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "title_override", length = 200)
    private String titleOverride;

    protected AgendaSlot() {}

    public AgendaSlot(Event event, Submission submission, EventTrack track,
                      Instant startsAt, Instant endsAt) {
        this.event = event;
        this.submission = submission;
        this.track = track;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    /** Slot sin charla (break, lunch, registration). */
    public static AgendaSlot breakSlot(Event event, EventTrack track,
                                       Instant startsAt, Instant endsAt, String label) {
        AgendaSlot slot = new AgendaSlot(event, null, track, startsAt, endsAt);
        slot.titleOverride = label;
        return slot;
    }

    public boolean overlapsWith(AgendaSlot other) {
        return !this.endsAt.isBefore(other.startsAt) && !this.startsAt.isAfter(other.endsAt);
    }

    public boolean isStrictlyBefore(Instant moment) {
        return this.endsAt.isBefore(moment);
    }

    public long durationMinutes() {
        return ChronoUnit.MINUTES.between(startsAt, endsAt);
    }

    public String displayTitle() {
        if (titleOverride != null && !titleOverride.isBlank()) return titleOverride;
        if (submission != null) return submission.getTitle();
        return "(Slot vacío)";
    }

    public UUID getId() { return id; }
    public Event getEvent() { return event; }
    public Submission getSubmission() { return submission; }
    public void setSubmission(Submission submission) { this.submission = submission; }
    public EventTrack getTrack() { return track; }
    public void setTrack(EventTrack track) { this.track = track; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public String getTitleOverride() { return titleOverride; }
    public void setTitleOverride(String titleOverride) { this.titleOverride = titleOverride; }
}
