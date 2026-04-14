package com.jugbaq.cfp.ui.admin;

import com.jugbaq.cfp.events.EventService;
import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventStatus;
import com.jugbaq.cfp.ui.layout.MainLayout;
import com.jugbaq.cfp.users.security.SecurityUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Route(value = "t/:tenantSlug/admin/events", layout = MainLayout.class)
@PageTitle("Gestión de Eventos")
@RolesAllowed({"ORGANIZER", "ADMIN"})
public class AdminEventsView extends VerticalLayout {

    private final EventService eventService;
    private final SecurityUtils securityUtils;
    private final Grid<Event> grid = new Grid<>(Event.class, false);

    public AdminEventsView(EventService eventService, SecurityUtils securityUtils) {
        this.eventService = eventService;
        this.securityUtils = securityUtils;

        setSizeFull();
        add(new H2("Gestión de Eventos"));

        Button newBtn = new Button("Nuevo evento", e -> openNewEventDialog());
        newBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(newBtn);

        configureGrid();
        add(grid);
        refresh();
    }

    private void configureGrid() {
        grid.addColumn(Event::getName).setHeader("Nombre").setAutoWidth(true);
        grid.addColumn(Event::getSlug).setHeader("Slug");
        grid.addColumn(e -> e.getEventDate().toString()).setHeader("Fecha");
        grid.addColumn(e -> e.getStatus().name()).setHeader("Estado");
        grid.addComponentColumn(this::buildActions).setHeader("Acciones");
        grid.setSizeFull();
    }

    private HorizontalLayout buildActions(Event event) {
        HorizontalLayout actions = new HorizontalLayout();
        for (EventStatus target : EventStatus.values()) {
            if (event.getStatus().canTransitionTo(target)) {
                Button btn = new Button("→ " + target.name(), e -> {
                    eventService.updateStatus(event.getId(), target);
                    Notification.show("Estado: " + target.name());
                    refresh();
                });
                btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                actions.add(btn);
            }
        }
        Button agendaBtn = new Button("📅 Agenda", e -> actions.getUI()
                .ifPresent(ui -> ui.navigate("t/jugbaq/admin/agenda/" + event.getSlug())));
        agendaBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        actions.add(agendaBtn);

        return actions;
    }

    private void openNewEventDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nuevo evento");

        TextField nameField = new TextField("Nombre");
        TextField slugField = new TextField("Slug (url-friendly)");
        DateTimePicker datePicker = new DateTimePicker("Fecha del evento");
        datePicker.setValue(LocalDateTime.now().plusMonths(1));

        FormLayout form = new FormLayout(nameField, slugField, datePicker);
        dialog.add(form);

        Button save = new Button("Crear", e -> {
            try {
                Instant eventDate =
                        datePicker.getValue().atZone(ZoneId.systemDefault()).toInstant();
                eventService.createEvent(
                        slugField.getValue(),
                        nameField.getValue(),
                        eventDate,
                        securityUtils.getCurrentUserId().orElseThrow());
                Notification.show("Evento creado");
                dialog.close();
                refresh();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(new Button("Cancelar", e -> dialog.close()), save);
        dialog.open();
    }

    private void refresh() {
        grid.setItems(eventService.listAll());
    }
}
