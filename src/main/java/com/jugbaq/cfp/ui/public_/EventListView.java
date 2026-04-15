package com.jugbaq.cfp.ui.public_;

import static com.jugbaq.cfp.shared.tenant.TenantRouteHelper.tenantPath;

import com.jugbaq.cfp.events.EventService;
import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.ui.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Route(value = "t/:tenantSlug/events", layout = MainLayout.class)
@PageTitle("Eventos")
@AnonymousAllowed
public class EventListView extends VerticalLayout {

    public EventListView(EventService eventService) {
        setSizeFull();
        setPadding(true);
        setAlignItems(FlexComponent.Alignment.CENTER); // Centramos el contenedor principal

        // --- Contenedor principal para limitar el ancho ---
        VerticalLayout container = new VerticalLayout();
        container.setMaxWidth("800px"); // Limita el ancho para pantallas grandes
        container.setPadding(false);

        // --- Sección: Próximos Eventos ---
        H2 openHeader = new H2("Próximos eventos con CFP abierto");
        openHeader.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.MEDIUM);
        container.add(openHeader);

        var openEvents = eventService.listCfpOpen();
        if (openEvents.isEmpty()) {
            container.add(createEmptyState("No hay convocatorias (CFP) abiertas en este momento."));
        } else {
            Div grid = new Div();
            grid.setWidthFull();
            grid.addClassNames(LumoUtility.Display.GRID, LumoUtility.Gap.MEDIUM);
            grid.getStyle().set("grid-template-columns", "repeat(auto-fill, minmax(320px, 1fr))");
            openEvents.forEach(e -> grid.add(buildCard(e)));
            container.add(grid);
        }

        // --- Sección: Todos los Eventos ---
        H2 allHeader = new H2("Todos los eventos");
        allHeader.addClassNames(LumoUtility.Margin.Top.XLARGE, LumoUtility.Margin.Bottom.MEDIUM);
        container.add(allHeader);

        var allEvents = eventService.listAll();
        if (allEvents.isEmpty()) {
            container.add(new Paragraph("Aún no se han creado eventos."));
        } else {
            allEvents.forEach(e -> container.add(buildCard(e)));
        }

        add(container);
    }

    private Div buildCard(Event event) {
        Div card = new Div();
        // Estilo de tarjeta moderna
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.LARGE,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL,
                LumoUtility.Margin.Bottom.MEDIUM);
        card.setWidthFull();
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("transition", "transform 0.2s ease, box-shadow 0.2s ease");

        // Efecto Hover (Levantamiento sutil al pasar el mouse)
        card.getElement()
                .executeJs(
                        "this.addEventListener('mouseenter', () => { this.style.boxShadow = 'var(--lumo-box-shadow-m)'; this.style.transform = 'translateY(-2px)'; });"
                                + "this.addEventListener('mouseleave', () => { this.style.boxShadow = 'var(--lumo-box-shadow-xs)'; this.style.transform = 'translateY(0)'; });");

        // --- Fila Superior: Título y Badge ---
        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);

        H3 title = new H3(event.getName());
        title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.XLARGE);

        // Etiqueta de Estado (Badge)
        Span statusBadge = new Span(event.getStatus().name());
        statusBadge.getElement().getThemeList().add("badge");
        if ("PUBLISHED".equals(event.getStatus().name())) {
            statusBadge.getElement().getThemeList().add("success"); // Verde
        } else {
            statusBadge.getElement().getThemeList().add("contrast"); // Gris oscuro
        }

        headerRow.add(title, statusBadge);
        card.add(headerRow);

        // --- Tagline (Descripción corta) ---
        if (event.getTagline() != null && !event.getTagline().isBlank()) {
            Paragraph tagline = new Paragraph(event.getTagline());
            tagline.addClassNames(
                    LumoUtility.Margin.NONE, LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.MEDIUM);
            card.add(tagline);
        }

        // --- Fila Inferior: Fecha e Íconos ---
        HorizontalLayout footerRow = new HorizontalLayout();
        footerRow.setWidthFull();
        footerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        footerRow.setAlignItems(FlexComponent.Alignment.END);
        footerRow.addClassNames(LumoUtility.Margin.Top.SMALL);

        // Fecha con ícono de calendario
        HorizontalLayout dateContainer = new HorizontalLayout();
        dateContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        dateContainer.addClassNames(LumoUtility.TextColor.TERTIARY, LumoUtility.FontSize.SMALL);

        Icon calendarIcon = VaadinIcon.CALENDAR.create();
        calendarIcon.setSize("16px");

        // Formatear la fecha bonito (ej: 12 may 2026)
        String formattedDate = "Fecha no definida";
        if (event.getEventDate() != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", new Locale("es", "ES"));
                // Asumiendo que getEventDate() devuelve un Instant
                formattedDate = formatter.format(event.getEventDate().atZone(ZoneId.of("America/Bogota")));
            } catch (Exception e) {
                // Fallback por si la fecha viene en otro formato
                formattedDate = event.getEventDate().toString();
            }
        }
        dateContainer.add(calendarIcon, new Span(formattedDate));

        footerRow.add(dateContainer);

        // --- Botón de Enviar Propuesta ---
        if (event.isCfpOpen()) {
            Button submitBtn = new Button("Enviar propuesta", VaadinIcon.PAPERPLANE.create());
            submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            // Redirigir al formulario
            submitBtn.addClickListener(
                    e -> submitBtn.getUI().ifPresent(ui -> ui.navigate(tenantPath("submit/" + event.getSlug()))));

            footerRow.add(submitBtn);
        }

        card.add(footerRow);
        return card;
    }

    private Component createEmptyState(String mensaje) {
        VerticalLayout emptyState = new VerticalLayout();
        emptyState.setAlignItems(FlexComponent.Alignment.CENTER);
        emptyState.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        emptyState.addClassNames(
                LumoUtility.Padding.XLARGE,
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Margin.Vertical.MEDIUM);
        emptyState.getStyle().set("border", "1px dashed var(--lumo-contrast-20pct)");

        Icon icon = VaadinIcon.MEGAPHONE.create();
        icon.setSize("48px");
        icon.addClassNames(LumoUtility.TextColor.TERTIARY);

        H3 title = new H3("Sin convocatorias");
        title.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.Margin.Bottom.NONE);

        Paragraph desc = new Paragraph(mensaje);
        desc.addClassNames(
                LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.XSMALL, LumoUtility.TextAlignment.CENTER);

        emptyState.add(icon, title, desc);
        return emptyState;
    }
}
