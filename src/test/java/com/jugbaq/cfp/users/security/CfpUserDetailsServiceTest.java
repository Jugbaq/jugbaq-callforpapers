package com.jugbaq.cfp.users.security;

import com.jugbaq.cfp.TestcontainersConfiguration;
import com.jugbaq.cfp.shared.domain.Tenant;
import com.jugbaq.cfp.shared.domain.TenantRepository;
import com.jugbaq.cfp.users.domain.TenantRole;
import com.jugbaq.cfp.users.domain.User;
import com.jugbaq.cfp.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CfpUserDetailsServiceTest {

    @Autowired CfpUserDetailsService service;
    @Autowired
    UserRepository userRepository;
    @Autowired TenantRepository tenantRepository;

    private Tenant jugbaq;

    @BeforeEach
    void setUp() {
        jugbaq = tenantRepository.findBySlug("jugbaq").orElseThrow();
    }

    @Test
    void should_load_seeded_admin_user() {
        UserDetails details = service.loadUserByUsername("admin@jugbaq.dev");

        assertThat(details.getUsername()).isEqualTo("admin@jugbaq.dev");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting("authority")
                .contains("ROLE_ADMIN", "ROLE_ORGANIZER");
    }

    @Test
    void should_load_user_with_tenant_specific_roles() {
        User user = new User("multi.role@test.com", "Multi Role");
        user.setPasswordHash("$2a$10$fakehash");
        user.assignRole(jugbaq, TenantRole.SPEAKER);
        user.assignRole(jugbaq, TenantRole.ORGANIZER);
        userRepository.save(user);

        CfpUserDetails details = (CfpUserDetails) service.loadUserByUsername("multi.role@test.com");

        assertThat(details.hasRoleInTenant(jugbaq.getId(), TenantRole.SPEAKER)).isTrue();
        assertThat(details.hasRoleInTenant(jugbaq.getId(), TenantRole.ORGANIZER)).isTrue();
        assertThat(details.hasRoleInTenant(jugbaq.getId(), TenantRole.ADMIN)).isFalse();
    }

    @Test
    void should_throw_when_user_not_found() {
        assertThatThrownBy(() -> service.loadUserByUsername("noexiste@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
