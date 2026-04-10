package com.jugbaq.cfp.shared.tenant;

import java.util.Optional;
import java.util.UUID;

/**
 * Almacena el tenant actual en el ThreadLocal del request.
 * Se setea por TenantFilter y se consume por Hibernate @Filter.
 */
public final class TenantContext {

    private TenantContext() {}

    private static final ThreadLocal<UUID> CURRENT_TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_TENANT_SLUG = new ThreadLocal<>();

    public static void set(UUID tenantId, String slug) {
        CURRENT_TENANT_ID.set(tenantId);
        CURRENT_TENANT_SLUG.set(slug);
    }

    public static Optional<UUID> getTenantId() {
        return Optional.ofNullable(CURRENT_TENANT_ID.get());
    }

    public static UUID requireTenantId() {
        return getTenantId()
                .orElseThrow(() -> new TenantNotResolvedException("No tenant in context"));
    }

    public static Optional<String> getTenantSlug() {
        return Optional.ofNullable(CURRENT_TENANT_SLUG.get());
    }

    public static void clear() {
        CURRENT_TENANT_ID.remove();
        CURRENT_TENANT_SLUG.remove();
    }
}
