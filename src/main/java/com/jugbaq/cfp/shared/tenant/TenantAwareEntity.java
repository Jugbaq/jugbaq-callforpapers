package com.jugbaq.cfp.shared.tenant;

import java.util.UUID;

/**
 * Interfaz marker para entidades que deben filtrarse por tenant.
 * Las entidades que la implementan DEBEN tener un campo tenant_id.
 */
public interface TenantAwareEntity {
    UUID getTenantId();
}
