package com.jugbaq.cfp.ui.public_;

import com.jugbaq.cfp.shared.tenant.TenantContext;
import com.jugbaq.cfp.ui.layout.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "t/:tenantSlug/events", layout = MainLayout.class)
@PageTitle("Eventos")
@AnonymousAllowed
public class EventListView extends VerticalLayout implements BeforeEnterObserver {

    private final H2 title = new H2();
    private final Paragraph info = new Paragraph();

    public EventListView() {
        add(title, info);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String slug = event.getRouteParameters().get("tenantSlug").orElse("?");
        String tenantId = TenantContext.getTenantId()
                .map(Object::toString)
                .orElse("no resuelto");

        title.setText("Eventos — " + slug);
        info.setText("Tenant ID desde TenantContext: " + tenantId);
    }
}
