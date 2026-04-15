package com.jugbaq.cfp.shared.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantRouteHelperTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void tenantPath_uses_slug_from_context() {
        TenantContext.set(UUID.randomUUID(), "myorg");

        assertThat(TenantRouteHelper.tenantPath("events")).isEqualTo("t/myorg/events");
    }

    @Test
    void tenantPath_falls_back_to_default_when_no_context() {
        assertThat(TenantRouteHelper.tenantPath("events")).isEqualTo("t/jugbaq/events");
    }

    @Test
    void absoluteTenantPath_prepends_slash() {
        TenantContext.set(UUID.randomUUID(), "myorg");

        assertThat(TenantRouteHelper.absoluteTenantPath("speakers")).isEqualTo("/t/myorg/speakers");
    }
}
