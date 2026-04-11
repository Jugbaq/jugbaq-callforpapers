package com.jugbaq.cfp.ui.public_;

import com.jugbaq.cfp.events.EventService;
import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.ui.layout.MainLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route(value = "t/:tenantSlug/events", layout = MainLayout.class)
@PageTitle("Eventos")
@AnonymousAllowed
public class EventListView extends VerticalLayout {

    public EventListView(EventService eventService) {
        setSizeFull();
        setPadding(true);

        add(new H2("Próximos eventos con CFP abierto"));

        var openEvents = eventService.listCfpOpen();
        if (openEvents.isEmpty()) {
            add(new Paragraph("No hay eventos con CFP abierto en este momento."));
        } else {
            openEvents.forEach(e -> add(buildCard(e)));
        }

        add(new H2("Todos los eventos"));
        eventService.listAll().forEach(e -> add(buildCard(e)));
    }

    private Div buildCard(Event event) {
        Div card = new Div();
        card.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Margin.Vertical.SMALL
        );
        card.add(new H3(event.getName()));
        if (event.getTagline() != null) card.add(new Paragraph(event.getTagline()));
        card.add(new Paragraph("Estado: " + event.getStatus()));
        card.add(new Paragraph("Fecha: " + event.getEventDate()));
        if (event.isCfpOpen()) {
            Anchor submit = new Anchor(
                    "/t/jugbaq/submit/" + event.getSlug(),
                    "→ Enviar propuesta"
            );
            card.add(submit);
        }
        return card;
    }
}
