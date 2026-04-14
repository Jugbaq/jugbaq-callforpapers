package com.jugbaq.cfp.users;

import com.jugbaq.cfp.users.domain.User;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record SpeakerSummary(
        UUID id,
        String fullName,
        String email,
        String tagline,
        String bio,
        String company,
        String jobTitle,
        String photoUrl,
        String city,
        String country,
        String websiteUrl,
        Map<String, String> socialLinks) {
    public static SpeakerSummary from(User user) {
        String tagline = null;
        String bio = null;
        String company = null;
        String jobTitle = null;
        String photoUrl = null;
        String city = null;
        String country = null;
        String websiteUrl = null;
        Map<String, String> socialLinks = new HashMap<>();

        if (user.getSpeakerProfile() != null) {
            tagline = user.getSpeakerProfile().getTagline();
            bio = user.getSpeakerProfile().getBio();
            company = user.getSpeakerProfile().getCompany();
            jobTitle = user.getSpeakerProfile().getJobTitle();
            photoUrl = user.getSpeakerProfile().getPhotoUrl();
            city = user.getSpeakerProfile().getCity();
            country = user.getSpeakerProfile().getCountry();
            websiteUrl = user.getSpeakerProfile().getWebsiteUrl();
            user.getSpeakerProfile()
                    .getSocialLinks()
                    .forEach(link -> socialLinks.put(link.getPlatform(), link.getUrl()));
        }

        return new SpeakerSummary(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                tagline,
                bio,
                company,
                jobTitle,
                photoUrl,
                city,
                country,
                websiteUrl,
                socialLinks);
    }
}
