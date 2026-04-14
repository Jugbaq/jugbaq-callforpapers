package com.jugbaq.cfp.users.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jugbaq.cfp.TestcontainersConfiguration;
import com.jugbaq.cfp.users.domain.User;
import com.jugbaq.cfp.users.domain.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SpeakerProfileServiceTest {

    @Autowired
    SpeakerProfileService profileService;

    @Autowired
    UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = new User("profile.test@jugbaq.dev", "Profile Test");
        user.setPasswordHash("$2a$10$fake");
        userRepository.save(user);
        userId = user.getId();
    }

    @Test
    void should_update_profile_with_valid_data() {
        SpeakerProfileService.ProfileUpdateData data = new SpeakerProfileService.ProfileUpdateData();
        data.setTagline("Senior Backend Dev");
        data.setBio("15 años trabajando con Java");
        data.setCompany("JUGBAQ");
        data.setJobTitle("Tech Lead");
        data.setCity("Barranquilla");
        data.setCountry("Colombia");
        data.setWebsiteUrl("https://geovanny.dev");

        var profile = profileService.updateProfile(userId, data);

        assertThat(profile.getTagline()).isEqualTo("Senior Backend Dev");
        assertThat(profile.getCity()).isEqualTo("Barranquilla");
        assertThat(profile.getWebsiteUrl()).isEqualTo("https://geovanny.dev");
    }

    @Test
    void should_reject_invalid_website_url() {
        SpeakerProfileService.ProfileUpdateData data = new SpeakerProfileService.ProfileUpdateData();
        data.setWebsiteUrl("not-a-url");

        assertThatThrownBy(() -> profileService.updateProfile(userId, data))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La URL debe iniciar con http o https");
    }

    @Test
    void should_trim_empty_strings_to_null() {
        SpeakerProfileService.ProfileUpdateData data = new SpeakerProfileService.ProfileUpdateData();
        data.setTagline("   ");
        data.setBio("");

        var profile = profileService.updateProfile(userId, data);

        assertThat(profile.getTagline()).isNull();
        assertThat(profile.getBio()).isNull();
    }

    @Test
    void should_replace_social_links() {
        profileService.replaceSocialLinks(
                userId,
                List.of(
                        new SpeakerProfileService.SocialLinkData("TWITTER", "https://twitter.com/geovanny"),
                        new SpeakerProfileService.SocialLinkData("GITHUB", "https://github.com/geovanny")));

        User reloaded = userRepository.findById(userId).orElseThrow();
        assertThat(reloaded.getSpeakerProfile().getSocialLinks()).hasSize(2);

        // Replace: 1 link nuevo, los anteriores se eliminan
        profileService.replaceSocialLinks(
                userId,
                List.of(new SpeakerProfileService.SocialLinkData("LINKEDIN", "https://linkedin.com/in/geovanny")));

        reloaded = userRepository.findById(userId).orElseThrow();
        assertThat(reloaded.getSpeakerProfile().getSocialLinks()).hasSize(1);
        assertThat(reloaded.getSpeakerProfile().getSocialLinks().getFirst().getPlatform())
                .isEqualTo("LINKEDIN");
    }

    @Test
    void should_reject_invalid_social_link_url() {
        assertThatThrownBy(() -> profileService.replaceSocialLinks(
                        userId, List.of(new SpeakerProfileService.SocialLinkData("TWITTER", "not-a-url"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_skip_empty_social_links() {
        profileService.replaceSocialLinks(
                userId,
                List.of(
                        new SpeakerProfileService.SocialLinkData("TWITTER", ""),
                        new SpeakerProfileService.SocialLinkData("GITHUB", "https://github.com/test")));

        User reloaded = userRepository.findById(userId).orElseThrow();
        assertThat(reloaded.getSpeakerProfile().getSocialLinks()).hasSize(1);
    }
}
