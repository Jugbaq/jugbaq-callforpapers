package com.jugbaq.cfp.submissions;

import com.jugbaq.cfp.submissions.domain.SubmissionLevel;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SubmissionData {
    private String title;
    private String abstractText;
    private String pitch;
    private SubmissionLevel level = SubmissionLevel.INTERMEDIATE;
    private UUID formatId;
    private UUID trackId;
    private Set<String> tags = new HashSet<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String a) { this.abstractText = a; }
    public String getPitch() { return pitch; }
    public void setPitch(String p) { this.pitch = p; }
    public SubmissionLevel getLevel() { return level; }
    public void setLevel(SubmissionLevel level) { this.level = level; }
    public UUID getFormatId() { return formatId; }
    public void setFormatId(UUID formatId) { this.formatId = formatId; }
    public UUID getTrackId() { return trackId; }
    public void setTrackId(UUID trackId) { this.trackId = trackId; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
}
