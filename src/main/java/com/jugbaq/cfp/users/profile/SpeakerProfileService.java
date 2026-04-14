package com.jugbaq.cfp.users.profile;

import com.jugbaq.cfp.shared.security.HtmlSanitizer;
import com.jugbaq.cfp.users.SpeakerSummary;
import com.jugbaq.cfp.users.domain.SpeakerProfile;
import com.jugbaq.cfp.users.domain.User;
import com.jugbaq.cfp.users.domain.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SpeakerProfileService {

    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[\\w-]+(?:\\.[\\w-]+)+[/#?]?.*$");
    private static final String USER_NOT_FOUND_MSG = "Usuario no encontrado";

    private final UserRepository userRepository;
    private final HtmlSanitizer sanitizer;

    public SpeakerProfileService(UserRepository userRepository, HtmlSanitizer sanitizer) {
        this.userRepository = userRepository;
        this.sanitizer = sanitizer;
    }

    public SpeakerProfile updateProfile(UUID userId, ProfileUpdateData data) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));

        user.initSpeakerProfile();
        SpeakerProfile profile = user.getSpeakerProfile();

        profile.setTagline(sanitizer.sanitizePlainText(trimOrNull(data.getTagline())));
        profile.setBio(sanitizer.sanitizeBasic(trimOrNull(data.getBio())));
        profile.setCompany(sanitizer.sanitizePlainText(trimOrNull(data.getCompany())));
        profile.setJobTitle(sanitizer.sanitizePlainText(trimOrNull(data.getJobTitle())));
        profile.setCity(sanitizer.sanitizePlainText(trimOrNull(data.getCity())));
        profile.setCountry(sanitizer.sanitizePlainText(trimOrNull(data.getCountry())));

        if (data.getWebsiteUrl() != null && !data.getWebsiteUrl().isBlank()) {
            validateUrl(data.getWebsiteUrl());
            profile.setWebsiteUrl(data.getWebsiteUrl().trim());
        } else {
            profile.setWebsiteUrl(null);
        }

        userRepository.save(user);
        return profile;
    }

    public void setAvatar(UUID userId, String relativePath) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));
        user.initSpeakerProfile();
        user.getSpeakerProfile().setPhotoUrl(relativePath);
        userRepository.save(user);
    }

    public void replaceSocialLinks(UUID userId, List<SocialLinkData> links) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));
        user.initSpeakerProfile();
        SpeakerProfile profile = user.getSpeakerProfile();

        profile.getSocialLinks().clear();
        for (SocialLinkData link : links) {
            if (link.getUrl() == null || link.getUrl().isBlank()) continue;
            validateUrl(link.getUrl());
            profile.addSocialLink(link.getPlatform(), link.getUrl().trim());
        }
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findUserForPublicProfile(UUID userId) {
        return userRepository.findById(userId);
    }

    private void validateUrl(String url) {
        if (!URL_PATTERN.matcher(url).matches()) {
            throw new IllegalArgumentException("URL inválida: " + url);
        }
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // --- DTOs ---

    public static class ProfileUpdateData {
        private String tagline;
        private String bio;
        private String company;
        private String jobTitle;
        private String city;
        private String country;
        private String websiteUrl;

        public String getTagline() {
            return tagline;
        }

        public void setTagline(String v) {
            this.tagline = v;
        }

        public String getBio() {
            return bio;
        }

        public void setBio(String v) {
            this.bio = v;
        }

        public String getCompany() {
            return company;
        }

        public void setCompany(String v) {
            this.company = v;
        }

        public String getJobTitle() {
            return jobTitle;
        }

        public void setJobTitle(String v) {
            this.jobTitle = v;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String v) {
            this.city = v;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String v) {
            this.country = v;
        }

        public String getWebsiteUrl() {
            return websiteUrl;
        }

        public void setWebsiteUrl(String v) {
            this.websiteUrl = v;
        }
    }

    public static class SocialLinkData {
        private String platform;
        private String url;

        public SocialLinkData() {}

        public SocialLinkData(String platform, String url) {
            this.platform = platform;
            this.url = url;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String v) {
            this.platform = v;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String v) {
            this.url = v;
        }
    }

    @Transactional(readOnly = true)
    public SpeakerSummary getSpeakerSummary(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));
        user.initSpeakerProfile();
        return SpeakerSummary.from(user);
    }
}
