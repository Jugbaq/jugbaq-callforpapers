package com.jugbaq.cfp.events.domain;

import com.jugbaq.cfp.shared.domain.BaseEntity;
import com.jugbaq.cfp.shared.tenant.TenantAwareEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Filter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "slug"}))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Event extends BaseEntity implements TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 300)
    private String tagline;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "event_date", nullable = false)
    private Instant eventDate;

    @Column(length = 300)
    private String location;

    @Column(name = "is_online", nullable = false)
    private boolean online = false;

    @Column(name = "cfp_opens_at")
    private Instant cfpOpensAt;

    @Column(name = "cfp_closes_at")
    private Instant cfpClosesAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "max_submissions_per_speaker", nullable = false)
    private int maxSubmissionsPerSpeaker = 3;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventTrack> tracks = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventSessionFormat> formats = new ArrayList<>();

    protected Event() {}

    public Event(UUID tenantId, String slug, String name, Instant eventDate, UUID createdBy) {
        this.tenantId = tenantId;
        this.slug = slug;
        this.name = name;
        this.eventDate = eventDate;
        this.createdBy = createdBy;
    }

    public void transitionTo(EventStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Transición inválida: " + status + " → " + newStatus);
        }
        this.status = newStatus;
    }

    public boolean isCfpOpen() {
        if (status != EventStatus.CFP_OPEN) return false;
        Instant now = Instant.now();
        if (cfpOpensAt != null && now.isBefore(cfpOpensAt)) return false;
        if (cfpClosesAt != null && now.isAfter(cfpClosesAt)) return false;
        return true;
    }

    public void addTrack(String name, String description) {
        tracks.add(new EventTrack(this, name, description));
    }

    public void addFormat(String name, int durationMinutes) {
        formats.add(new EventSessionFormat(this, name, durationMinutes));
    }

    // Getters / setters esenciales
    public UUID getId() { return id; }
    @Override public UUID getTenantId() { return tenantId; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getEventDate() { return eventDate; }
    public void setEventDate(Instant eventDate) { this.eventDate = eventDate; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public Instant getCfpOpensAt() { return cfpOpensAt; }
    public void setCfpOpensAt(Instant v) { this.cfpOpensAt = v; }
    public Instant getCfpClosesAt() { return cfpClosesAt; }
    public void setCfpClosesAt(Instant v) { this.cfpClosesAt = v; }
    public EventStatus getStatus() { return status; }
    public int getMaxSubmissionsPerSpeaker() { return maxSubmissionsPerSpeaker; }
    public void setMaxSubmissionsPerSpeaker(int v) { this.maxSubmissionsPerSpeaker = v; }
    public UUID getCreatedBy() { return createdBy; }
    public List<EventTrack> getTracks() { return tracks; }
    public List<EventSessionFormat> getFormats() { return formats; }
}
