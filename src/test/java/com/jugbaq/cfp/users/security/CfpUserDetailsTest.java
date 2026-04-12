package com.jugbaq.cfp.users.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.jugbaq.cfp.users.TenantRole;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CfpUserDetailsTest {

    @Test
    void should_build_authorities_from_all_tenants() {
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        Map<UUID, Set<TenantRole>> roles = Map.of(
                tenant1, Set.of(TenantRole.SPEAKER),
                tenant2, Set.of(TenantRole.ADMIN, TenantRole.ORGANIZER));

        CfpUserDetails details = new CfpUserDetails(UUID.randomUUID(), "test@test.com", "hash", "Test", true, roles);

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_SPEAKER", "ROLE_ADMIN", "ROLE_ORGANIZER");
    }

    @Test
    void should_check_role_per_tenant() {
        UUID jugbaqId = UUID.randomUUID();
        UUID devjvmId = UUID.randomUUID();

        Map<UUID, Set<TenantRole>> roles = Map.of(
                jugbaqId, Set.of(TenantRole.SPEAKER),
                devjvmId, Set.of(TenantRole.ADMIN));

        CfpUserDetails details = new CfpUserDetails(UUID.randomUUID(), "test@test.com", "hash", "Test", true, roles);

        assertThat(details.hasRoleInTenant(jugbaqId, TenantRole.SPEAKER)).isTrue();
        assertThat(details.hasRoleInTenant(jugbaqId, TenantRole.ADMIN)).isFalse();
        assertThat(details.hasRoleInTenant(devjvmId, TenantRole.ADMIN)).isTrue();
    }
}
