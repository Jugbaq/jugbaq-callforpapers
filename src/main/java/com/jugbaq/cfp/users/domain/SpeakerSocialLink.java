package com.jugbaq.cfp.users.domain;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Table(name = "speaker_social_links")
public class SpeakerSocialLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String platform;

    @Column(nullable = false, length = 500)
    private String url;

    protected SpeakerSocialLink() {} // JPA

    public SpeakerSocialLink(UUID userId, String platform, String url) {
        this.userId = userId;
        this.platform = platform;
        this.url = url;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getPlatform() { return platform; }
    public String getUrl() { return url; }
}
