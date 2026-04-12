package com.jugbaq.cfp.shared.tenant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void should_store_and_retrieve_tenant() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId, "jugbaq");

        assertThat(TenantContext.getTenantId()).hasValue(tenantId);
        assertThat(TenantContext.getTenantSlug()).hasValue("jugbaq");
    }

    @Test
    void should_return_empty_when_no_tenant_set() {
        assertThat(TenantContext.getTenantId()).isEmpty();
        assertThat(TenantContext.getTenantSlug()).isEmpty();
    }

    @Test
    void requireTenantId_should_throw_when_not_set() {
        assertThatThrownBy(TenantContext::requireTenantId)
                .isInstanceOf(TenantNotResolvedException.class)
                .hasMessageContaining("No tenant in context");
    }

    @Test
    void clear_should_remove_tenant() {
        TenantContext.set(UUID.randomUUID(), "test");
        TenantContext.clear();

        assertThat(TenantContext.getTenantId()).isEmpty();
    }
}
