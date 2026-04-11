package com.jugbaq.cfp.submissions.domain;

import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventSessionFormat;
import com.jugbaq.cfp.events.domain.EventTrack;
import com.jugbaq.cfp.shared.domain.BaseEntity;
import com.jugbaq.cfp.shared.tenant.TenantAwareEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "submissions")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Submission extends BaseEntity implements TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "speaker_id", nullable = false)
    private UUID speakerId;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(name = "abstract", nullable = false, columnDefinition = "text")
    private String abstractText;

    @Column(columnDefinition = "text")
    private String pitch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionLevel level = SubmissionLevel.INTERMEDIATE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "format_id")
    private EventSessionFormat format;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private EventTrack track;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.DRAFT;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @ElementCollection
    @CollectionTable(name = "submission_tags",
            joinColumns = @JoinColumn(name = "submission_id"))
    @Column(name = "tag", length = 50)
    private Set<String> tags = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "submission_co_speakers",
            joinColumns = @JoinColumn(name = "submission_id"))
    @Column(name = "user_id")
    private Set<UUID> coSpeakers = new HashSet<>();

    protected Submission() {}

    public Submission(UUID tenantId, Event event, UUID speakerId, String title, String abstractText) {
        this.tenantId = tenantId;
        this.event = event;
        this.speakerId = speakerId;
        this.title = title;
        this.abstractText = abstractText;
    }

    // --- Métodos de dominio ---

    public void transitionTo(SubmissionStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidSubmissionTransitionException(status, newStatus);
        }
        if (newStatus == SubmissionStatus.SUBMITTED && submittedAt == null) {
            submittedAt = Instant.now();
        }
        this.status = newStatus;
    }

    public void assertOwnedBy(UUID userId) {
        if (!speakerId.equals(userId)) {
            throw new NotSubmissionOwnerException();
        }
    }

    public void updateContent(String title, String abstractText, String pitch,
                              SubmissionLevel level, Set<String> tags) {
        if (!status.isEditableBySpeaker()) {
            throw new IllegalStateException(
                    "No se puede editar una propuesta en estado " + status);
        }
        this.title = title;
        this.abstractText = abstractText;
        this.pitch = pitch;
        this.level = level;
        this.tags = new HashSet<>(tags);
    }

    // --- Getters ---
    public UUID getId() { return id; }
    @Override public UUID getTenantId() { return tenantId; }
    public Event getEvent() { return event; }
    public UUID getSpeakerId() { return speakerId; }
    public String getTitle() { return title; }
    public String getAbstractText() { return abstractText; }
    public String getPitch() { return pitch; }
    public SubmissionLevel getLevel() { return level; }
    public EventSessionFormat getFormat() { return format; }
    public void setFormat(EventSessionFormat format) { this.format = format; }
    public EventTrack getTrack() { return track; }
    public void setTrack(EventTrack track) { this.track = track; }
    public SubmissionStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Set<String> getTags() { return tags; }
    public Set<UUID> getCoSpeakers() { return coSpeakers; }
}
