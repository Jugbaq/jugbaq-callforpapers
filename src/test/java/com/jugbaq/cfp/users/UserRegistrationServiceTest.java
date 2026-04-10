package com.jugbaq.cfp.users;

import com.jugbaq.cfp.TestcontainersConfiguration;
import com.jugbaq.cfp.shared.domain.TenantRepository;

import com.jugbaq.cfp.users.domain.TenantRole;
import com.jugbaq.cfp.users.domain.User;
import com.jugbaq.cfp.users.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserRegistrationServiceTest {

    @Autowired
    UserRepository userRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void should_register_speaker_with_encoded_password() {
        User user = new User("nuevo.speaker@test.com", "Nuevo Speaker");
        user.setPasswordHash(passwordEncoder.encode("miPassword123"));
        user.initSpeakerProfile();

        tenantRepository.findBySlug("jugbaq").ifPresent(tenant ->
                user.assignRole(tenant, TenantRole.SPEAKER)
        );

        userRepository.save(user);

        User found = userRepository.findByEmailIgnoreCase("nuevo.speaker@test.com").orElseThrow();
        assertThat(found.getPasswordHash()).isNotEqualTo("miPassword123");
        assertThat(passwordEncoder.matches("miPassword123", found.getPasswordHash())).isTrue();
        assertThat(found.getSpeakerProfile()).isNotNull();
        assertThat(found.getTenantRoles()).hasSize(1);
        assertThat(found.hasRole(
                tenantRepository.findBySlug("jugbaq").orElseThrow().getId(),
                TenantRole.SPEAKER
        )).isTrue();
    }

    @Test
    void should_reject_duplicate_email() {
        userRepository.save(new User("duplicado@test.com", "Primero"));

        assertThat(userRepository.existsByEmailIgnoreCase("duplicado@test.com")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("DUPLICADO@TEST.COM")).isTrue();
    }
}
