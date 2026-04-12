package com.jugbaq.cfp.shared.tenant;

import com.jugbaq.cfp.shared.domain.TenantRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private static final Pattern TENANT_PATH_PATTERN = Pattern.compile("^/t/([a-z0-9\\-]+)(/.*)?$");

    private final TenantRepository tenantRepository;

    public TenantFilter(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            String path = httpReq.getRequestURI();

            Matcher matcher = TENANT_PATH_PATTERN.matcher(path);
            if (matcher.matches()) {
                String slug = matcher.group(1);
                resolveTenantBySlug(slug);
            } else {
                restoreTenantFromSession(httpReq);
            }

            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void resolveTenantBySlug(String slug) {
        tenantRepository
                .findBySlug(slug)
                .ifPresentOrElse(
                        tenant -> {
                            TenantContext.set(tenant.getId(), tenant.getSlug());
                            log.debug("Tenant resolved from URL: {} ({})", tenant.getName(), slug);
                        },
                        () -> log.warn("Unknown tenant slug: {}", slug));
    }

    private void restoreTenantFromSession(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) {
            return;
        }
        String slug = (String) session.getAttribute("cfp_tenant_slug");
        if (slug != null) {
            tenantRepository.findBySlug(slug).ifPresent(tenant -> {
                TenantContext.set(tenant.getId(), tenant.getSlug());
                log.debug("Tenant restored from session: {} ({})", tenant.getName(), slug);
            });
        }
    }
}
