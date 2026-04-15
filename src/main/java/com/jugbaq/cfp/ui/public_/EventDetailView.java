package com.jugbaq.cfp.ui.public_;

import static com.jugbaq.cfp.shared.tenant.TenantRouteHelper.absoluteTenantPath;

import com.jugbaq.cfp.events.EventService;
import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventTrack;
import com.jugbaq.cfp.publishing.AgendaService;
import com.jugbaq.cfp.publishing.AgendaSlotSummary;
import com.jugbaq.cfp.ui.layout.MainLayout;
import com.jugbaq.cfp.users.SpeakerSummary;
import com.jugbaq.cfp.users.UserQueryService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Route(value = "t/:tenantSlug/events/:eventSlug", layout = MainLayout.class)
@PageTitle("Evento")
@AnonymousAllowed
public class EventDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final EventService eventService;
    private final AgendaService agendaService;
    private final UserQueryService userQueryService;

    public EventDetailView(EventService eventService, AgendaService agendaService, UserQueryService userQueryService) {
        this.eventService = eventService;
        this.agendaService = agendaService;
        this.userQueryService = userQueryService;
        setMaxWidth("1200px");
        getStyle().set("margin", "0 auto");
        setPadding(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        String slug = beforeEnterEvent.getRouteParameters().get("eventSlug").orElse(null);
        // Buscamos forzando la carga de los tracks para evitar el LazyException
        Event event = eventService.getEventWithTracksBySlug(slug);

        if (event == null) {
            add(new H1("Evento no encontrado"));
            return;
        }

        renderHeader(event);
        renderAgendaGrid(event); // <-- Nuevo método de la matriz
    }

    private void renderHeader(Event event) {
        H1 name = new H1(event.getName());
        name.getStyle().set("color", "var(--lumo-primary-color)");
        add(name);

        if (event.getTagline() != null && !event.getTagline().isBlank()) {
            Paragraph tagline = new Paragraph(event.getTagline());
            tagline.getStyle().set("font-size", "var(--lumo-font-size-l)");
            add(tagline);
        }

        // --- META INFO: Cero Emojis, 100% VaadinIcons ---
        HorizontalLayout metaContainer = new HorizontalLayout();
        metaContainer.setAlignItems(Alignment.CENTER);
        metaContainer.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.MEDIUM);

        // 1. Icono de Fecha
        Icon calIcon = VaadinIcon.CALENDAR.create();
        calIcon.setSize("16px");
        Span dateSpan = new Span(calIcon, new Span(formatDate(event.getEventDate())));
        dateSpan.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL);
        metaContainer.add(dateSpan);

        // 2. Icono de Ubicación
        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            Icon locIcon = VaadinIcon.MAP_MARKER.create();
            locIcon.setSize("16px");
            Span locSpan = new Span(locIcon, new Span(event.getLocation()));
            locSpan.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL);
            metaContainer.add(locSpan);
        }

        // 3. Icono de Modalidad Online
        if (event.isOnline()) {
            Icon onlineIcon = VaadinIcon.LAPTOP.create();
            onlineIcon.setSize("16px");
            Span onlineSpan = new Span(onlineIcon, new Span("Online"));
            onlineSpan.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL);
            metaContainer.add(onlineSpan);
        }

        add(metaContainer);

        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            Paragraph desc = new Paragraph(event.getDescription());
            desc.getStyle().set("white-space", "pre-wrap");
            add(desc);
        }

        // --- BOTÓN DE DESCARGA ICAL PROFESIONAL ---
        Button icalBtn = new Button("Añadir al calendario (.ics)", VaadinIcon.DOWNLOAD.create());
        icalBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Anchor icalLink = new Anchor(absoluteTenantPath("api/events/" + event.getSlug() + "/calendar.ics"), "");
        icalLink.add(icalBtn); // Metemos el botón nativo dentro del link
        icalLink.getElement().setAttribute("download", true);
        icalLink.getStyle().set("margin", "16px 0");

        add(icalLink);
    }

    private void renderAgendaGrid(Event event) {
        add(new H2("Agenda"));

        var slots = agendaService.listForEvent(event.getId()).stream()
                .sorted(Comparator.comparing(AgendaSlotSummary::startsAt))
                .toList();

        if (slots.isEmpty()) {
            add(new Paragraph("La agenda aún no está publicada."));
            return;
        }

        List<EventTrack> tracks = event.getTracks();
        int numTracks = Math.max(1, tracks.size());

        // --- CONTENEDOR GRID PRINCIPAL ---
        Div grid = new Div();
        grid.setWidthFull();
        grid.getStyle()
                .set("display", "grid")
                .set("gap", "1rem")
                // Columna 1: Hora (100px). Columnas 2 a N: Tracks (Mínimo 250px para que no se aprieten)
                .set("grid-template-columns", "80px repeat(" + numTracks + ", minmax(280px, 1fr))");

        // Envolvemos el grid en un div con scroll por si lo abren en celular
        Div scrollWrapper = new Div(grid);
        scrollWrapper.setWidthFull();
        scrollWrapper.getStyle().set("overflow-x", "auto").set("padding-bottom", "1rem");

        // --- FILA 0: ENCABEZADOS DE LOS TRACKS ---
        grid.add(new Div()); // Esquina superior izquierda vacía
        for (EventTrack track : tracks) {
            Div header = new Div(new Span(track.getName()));
            header.addClassNames(
                    LumoUtility.TextAlignment.CENTER,
                    LumoUtility.FontWeight.BOLD,
                    LumoUtility.Padding.SMALL,
                    LumoUtility.Background.CONTRAST_5,
                    LumoUtility.BorderRadius.SMALL);
            header.getStyle().set("color", "var(--lumo-primary-color)");
            grid.add(header);
        }

        // --- AGRUPAR SLOTS POR HORA DE INICIO ---
        Map<Instant, List<AgendaSlotSummary>> slotsByTime = slots.stream()
                .collect(Collectors.groupingBy(AgendaSlotSummary::startsAt, TreeMap::new, Collectors.toList()));

        // --- PINTAR FILAS ---
        for (Map.Entry<Instant, List<AgendaSlotSummary>> entry : slotsByTime.entrySet()) {
            Instant startTime = entry.getKey();
            List<AgendaSlotSummary> concurrentSlots = entry.getValue();

            // 1. Pintar la celda de la hora (Columna 1)
            Div timeCell = new Div(new Span(formatTime(startTime)));
            timeCell.addClassNames(
                    LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.TERTIARY, LumoUtility.TextAlignment.RIGHT);
            timeCell.getStyle().set("padding-right", "1rem").set("margin-top", "0.5rem");
            grid.add(timeCell);

            // 2. Revisar si hay un slot "Global" (Sin track, ej: Almuerzo)
            AgendaSlotSummary globalSlot = concurrentSlots.stream()
                    .filter(s -> s.trackId() == null)
                    .findFirst()
                    .orElse(null);

            if (globalSlot != null) {
                // Pintar franja horizontal que atraviesa todos los tracks
                Div card = buildSlotCard(globalSlot, true);
                card.getStyle().set("grid-column", "2 / -1"); // Magia: expandir hasta el final
                grid.add(card);
            } else {
                // Pintar slots regulares en su respectiva columna de Track
                for (AgendaSlotSummary slot : concurrentSlots) {
                    Div card = buildSlotCard(slot, false);

                    // Buscar el índice del track (+2 porque CSS Grid empieza en 1 y la hora es la col 1)
                    int trackIndex =
                            tracks.stream().map(EventTrack::getId).toList().indexOf(slot.trackId());
                    if (trackIndex >= 0) {
                        card.getStyle().set("grid-column", String.valueOf(trackIndex + 2));
                        grid.add(card);
                    }
                }
            }
        }

        add(scrollWrapper);
    }

    private Div buildSlotCard(AgendaSlotSummary slot, boolean isGlobal) {
        Div card = new Div();
        card.addClassNames(
                LumoUtility.Padding.MEDIUM,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.XSMALL);

        if (isGlobal) {
            // Estilo para breaks / almuerzos (Gris sutil, centrado)
            card.addClassNames(
                    LumoUtility.Background.CONTRAST_5,
                    LumoUtility.AlignItems.CENTER,
                    LumoUtility.JustifyContent.CENTER);
            card.getStyle().set("border", "1px dashed var(--lumo-contrast-20pct)");

            // Creamos el ícono oficial de Vaadin (CUP es ideal para recesos)
            Icon breakIcon = VaadinIcon.TIMER.create();
            breakIcon.setSize("16px");
            breakIcon.getStyle().set("margin-right", "8px");

            // Envolvemos todo en un Span con Flexbox para alinear perfecto
            Span titleSpan = new Span(breakIcon, new Span(slot.displayTitle()));
            titleSpan.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.AlignItems.CENTER,
                    LumoUtility.Margin.NONE,
                    LumoUtility.FontWeight.BOLD,
                    LumoUtility.TextColor.SECONDARY);

            card.add(titleSpan);
        } else {
            // Estilo para Charlas Normales (Fondo blanco, borde a la izquierda)
            card.addClassNames(LumoUtility.Background.BASE);
            card.getStyle()
                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("box-shadow", "var(--lumo-box-shadow-xs)")
                    .set("border-left", "4px solid var(--lumo-primary-color)");

            // Hora
            Span time = new Span(formatTime(slot.startsAt()) + " — " + formatTime(slot.endsAt()));
            time.addClassNames(
                    LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY, LumoUtility.FontWeight.BOLD);
            card.add(time);

            // Título
            Paragraph title = new Paragraph(slot.displayTitle());
            title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.SEMIBOLD);
            title.getStyle().set("line-height", "1.2");
            card.add(title);

            // Nombre del Speaker
            if (slot.speakerId() != null) {
                SpeakerSummary speaker = userQueryService.getSpeakerInfo(slot.speakerId());
                if (speaker != null) {
                    Icon userIcon = VaadinIcon.USER.create();
                    userIcon.setSize("12px");
                    userIcon.getStyle().set("margin-right", "4px");

                    Span speakerName = new Span(userIcon, new Span(speaker.fullName()));
                    speakerName.addClassNames(
                            LumoUtility.Display.FLEX,
                            LumoUtility.AlignItems.CENTER,
                            LumoUtility.Margin.NONE,
                            LumoUtility.FontSize.SMALL,
                            LumoUtility.TextColor.SECONDARY);
                    card.add(speakerName);
                }
            }
        }

        return card;
    }

    private String formatDate(java.time.Instant instant) {
        return instant.atZone(ZoneId.of("America/Bogota"))
                .format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy"));
    }

    private String formatTime(java.time.Instant instant) {
        return instant.atZone(ZoneId.of("America/Bogota")).format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
