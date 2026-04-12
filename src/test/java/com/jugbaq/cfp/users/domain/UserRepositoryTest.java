package com.jugbaq.cfp.users.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.jugbaq.cfp.TestcontainersConfiguration;
import com.jugbaq.cfp.shared.domain.Tenant;
import com.jugbaq.cfp.shared.domain.TenantRepository;
import com.jugbaq.cfp.users.TenantRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    TenantRepository tenantRepository;

    private Tenant jugbaq;

    @BeforeEach
    void setUp() {
        // El tenant jugbaq viene seedeado por Flyway V1
        jugbaq = tenantRepository.findBySlug("jugbaq").orElseThrow();
    }

    @Test
    void should_create_and_find_user_by_email() {
        User user = new User("speaker@jugbaq.dev", "Geovanny Mendoza");
        user.setPasswordHash("$2a$10$fakehash");
        userRepository.save(user);

        var found = userRepository.findByEmailIgnoreCase("speaker@jugbaq.dev");
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Geovanny Mendoza");
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    void should_find_user_by_email_case_insensitive() {
        userRepository.save(new User("Test@JugBaq.Dev", "Test User"));

        assertThat(userRepository.findByEmailIgnoreCase("test@jugbaq.dev")).isPresent();
        assertThat(userRepository.findByEmailIgnoreCase("TEST@JUGBAQ.DEV")).isPresent();
    }

    @Test
    void should_assign_role_to_user_in_tenant() {
        // Usamos un correo diferente para no chocar con el admin seedeado por Flyway
        User user = new User("test_admin@jugbaq.dev", "Admin User Test");
        user.assignRole(jugbaq, TenantRole.ADMIN);
        user.assignRole(jugbaq, TenantRole.ORGANIZER);
        userRepository.save(user);

        User found =
                userRepository.findByEmailIgnoreCase("test_admin@jugbaq.dev").orElseThrow();
        assertThat(found.getTenantRoles()).hasSize(2);
        assertThat(found.hasRole(jugbaq.getId(), TenantRole.ADMIN)).isTrue();
        assertThat(found.hasRole(jugbaq.getId(), TenantRole.SPEAKER)).isFalse();
    }

    @Test
    void should_create_user_with_oauth_account() {
        User user = new User("oauth@gmail.com", "OAuth User");
        user.addOAuthAccount("GOOGLE", "google-uid-12345");
        userRepository.save(user);

        var found = userRepository.findByOAuthProviderAndId("GOOGLE", "google-uid-12345");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("oauth@gmail.com");
    }

    @Test
    void should_create_user_with_speaker_profile() {
        User user = new User("speaker2@jugbaq.dev", "Speaker Con Perfil");
        user.initSpeakerProfile();
        user.getSpeakerProfile().setBio("Desarrollador Java apasionado");
        user.getSpeakerProfile().setCity("Barranquilla");
        user.getSpeakerProfile().setCountry("Colombia");
        user.getSpeakerProfile().setCompany("JUGBAQ");
        userRepository.save(user);

        User found = userRepository.findByEmailIgnoreCase("speaker2@jugbaq.dev").orElseThrow();
        assertThat(found.getSpeakerProfile()).isNotNull();
        assertThat(found.getSpeakerProfile().getCity()).isEqualTo("Barranquilla");
    }

    @Test
    void should_return_false_when_email_not_exists() {
        assertThat(userRepository.existsByEmailIgnoreCase("noexiste@test.com")).isFalse();
    }
}
