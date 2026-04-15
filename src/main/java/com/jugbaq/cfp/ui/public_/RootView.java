package com.jugbaq.cfp.ui.public_;

import com.jugbaq.cfp.shared.domain.TenantRepository;
import com.jugbaq.cfp.shared.tenant.TenantRouteHelper;
import com.jugbaq.cfp.users.security.CfpUserDetails;
import com.jugbaq.cfp.users.security.SecurityUtils;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("")
@AnonymousAllowed
public class RootView extends VerticalLayout implements BeforeEnterObserver {

    private final SecurityUtils securityUtils;
    private final TenantRepository tenantRepository;

    public RootView(SecurityUtils securityUtils, TenantRepository tenantRepository) {
        this.securityUtils = securityUtils;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // 1. Si el usuario YA inició sesión, lo mandamos directo a los eventos
        //    Resolvemos el tenant del usuario para construir la ruta correcta,
        //    incluso si TenantContext no está poblado (e.g. sesión perdida).
        if (securityUtils.getAuthenticatedUser().isPresent()) {
            CfpUserDetails user = securityUtils.getAuthenticatedUser().orElseThrow();
            String path = user.getRolesByTenant().entrySet().stream()
                    .filter(e -> !e.getValue().isEmpty())
                    .findFirst()
                    .flatMap(e -> tenantRepository.findById(e.getKey()).map(t -> t.getSlug()))
                    .map(slug -> "/t/" + slug + "/events")
                    .orElse(TenantRouteHelper.absoluteTenantPath("events"));
            event.forwardTo(path);
        }
        // 2. Si NO ha iniciado sesión (es un anónimo), lo tiramos de cabeza al Login
        else {
            event.forwardTo(LoginView.class);
        }
    }
}
