package com.jugbaq.cfp.shared.tenant;

/**
 * Helper para construir rutas de navegación con el slug del tenant actual.
 * Lee el slug de {@link TenantContext} (populado por {@link TenantFilter}).
 */
public final class TenantRouteHelper {

    private static final String DEFAULT_SLUG = "jugbaq";

    private TenantRouteHelper() {}

    /**
     * Construye una ruta relativa con el slug del tenant actual.
     * Ejemplo: {@code tenantPath("events")} → {@code "t/jugbaq/events"}
     *
     * @param relativePath la ruta sin el prefijo de tenant (sin leading slash)
     * @return la ruta completa con prefijo {@code t/{slug}/}
     */
    public static String tenantPath(String relativePath) {
        String slug = TenantContext.getTenantSlug().orElse(DEFAULT_SLUG);
        return "t/" + slug + "/" + relativePath;
    }

    /**
     * Construye una ruta absoluta (con leading slash) con el slug del tenant actual.
     * Ejemplo: {@code absoluteTenantPath("events")} → {@code "/t/jugbaq/events"}
     */
    public static String absoluteTenantPath(String relativePath) {
        return "/" + tenantPath(relativePath);
    }
}
