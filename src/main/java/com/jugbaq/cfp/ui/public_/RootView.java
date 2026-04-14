package com.jugbaq.cfp.ui.public_;

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

    public RootView(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // 1. Si el usuario YA inició sesión, lo mandamos directo a los eventos
        if (securityUtils.getAuthenticatedUser().isPresent()) {
            event.forwardTo("/t/jugbaq/events");
        }
        // 2. Si NO ha iniciado sesión (es un anónimo), lo tiramos de cabeza al Login
        else {
            event.forwardTo(LoginView.class);
        }
    }
}
