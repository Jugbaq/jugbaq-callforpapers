package com.jugbaq.cfp.shared.tenant;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Guarda el tenant slug en la sesión HTTP para que sobreviva
 * al redirect de OAuth2.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 11) // justo después de TenantFilter
public class TenantSessionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        TenantContext.getTenantSlug().ifPresent(slug -> {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            httpReq.getSession().setAttribute("cfp_tenant_slug", slug);
        });

        chain.doFilter(request, response);
    }
}
