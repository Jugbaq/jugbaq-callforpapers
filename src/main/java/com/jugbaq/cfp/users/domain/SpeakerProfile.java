package com.jugbaq.cfp.users.domain;

import com.jugbaq.cfp.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "speaker_profiles")
public class SpeakerProfile extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 200)
    private String tagline;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(length = 150)
    private String company;

    @Column(name = "job_title", length = 150)
    private String jobTitle;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private List<SpeakerSocialLink> socialLinks = new ArrayList<>();

    protected SpeakerProfile() {} // JPA

    public SpeakerProfile(User user) {
        this.user = user;
    }

    // --- Método de dominio ---

    public void addSocialLink(String platform, String url) {
        this.socialLinks.add(new SpeakerSocialLink(this.userId, platform, url));
    }

    // --- Getters / Setters ---

    public UUID getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public List<SpeakerSocialLink> getSocialLinks() {
        return Collections.unmodifiableList(socialLinks);
    }
}
