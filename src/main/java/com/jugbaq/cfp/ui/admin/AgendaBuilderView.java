package com.jugbaq.cfp.ui.admin;

import static com.jugbaq.cfp.shared.tenant.TenantRouteHelper.tenantPath;

import com.jugbaq.cfp.events.EventService;
import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventTrack;
import com.jugbaq.cfp.publishing.AgendaService;
import com.jugbaq.cfp.publishing.AgendaSlotSummary;
import com.jugbaq.cfp.publishing.domain.AgendaConflictException;
import com.jugbaq.cfp.publishing.domain.AgendaIncompleteException;
import com.jugbaq.cfp.submissions.SubmissionService;
import com.jugbaq.cfp.submissions.domain.Submission;
import com.jugbaq.cfp.submissions.domain.SubmissionStatus;
import com.jugbaq.cfp.ui.layout.MainLayout;
import com.jugbaq.cfp.users.UserQueryService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Route(value = "t/:tenantSlug/admin/agenda/:eventSlug", layout = MainLayout.class)
@PageTitle("Agenda Builder")
@RolesAllowed({"ORGANIZER", "ADMIN"})
public class AgendaBuilderView extends VerticalLayout implements BeforeEnterObserver {

    private final EventService eventService;
    private final AgendaService agendaService;
    private final SubmissionService submissionService;
    private final UserQueryService userQueryService;

    private Event event;
    private final VerticalLayout pendingPanel = new VerticalLayout();
    private final HorizontalLayout tracksPanel = new HorizontalLayout();
    private final H2 title = new H2();

    public AgendaBuilderView(
            EventService eventService,
            AgendaService agendaService,
            SubmissionService submissionService,
            UserQueryService userQueryService) {
        this.eventService = eventService;
        this.agendaService = agendaService;
        this.submissionService = submissionService;
        this.userQueryService = userQueryService;

        setSizeFull();
        setPadding(true);

        add(title);

        Button publishBtn = new Button("📢 Publicar evento", e -> publish());
        publishBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        add(publishBtn);

        pendingPanel.setWidth("280px");
        pendingPanel
                .getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        tracksPanel.setSizeFull();
        tracksPanel.setSpacing(true);

        HorizontalLayout main = new HorizontalLayout(pendingPanel, tracksPanel);
        main.setSizeFull();
        main.setSpacing(true);
        addAndExpand(main);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        String slug = beforeEnterEvent.getRouteParameters().get("eventSlug").orElse(null);
        if (slug == null) {
            beforeEnterEvent.rerouteTo(tenantPath("admin/events"));
            return;
        }

        event = eventService.getEventWithTracksBySlug(slug);

        if (event == null) {
            title.setText("Evento no encontrado");
            return;
        }

        title.setText("Agenda Builder — " + event.getName());
        refresh();
    }

    private void refresh() {
        renderPendingList();
        renderTracks();
    }

    private void renderPendingList() {
        pendingPanel.removeAll();
        pendingPanel.add(new H3("Pendientes"));
        pendingPanel.add(new Paragraph("Arrastra a un track →"));

        Set<UUID> alreadySlotted = new HashSet<>();
        agendaService.listForEvent(event.getId()).stream()
                .filter(s -> s.submissionId() != null)
                .forEach(s -> alreadySlotted.add(s.submissionId()));

        List<Submission> accepted = submissionService.listForReview(event.getId(), null).stream()
                .filter(s -> s.getStatus() == SubmissionStatus.ACCEPTED || s.getStatus() == SubmissionStatus.CONFIRMED)
                .filter(s -> !alreadySlotted.contains(s.getId()))
                .toList();

        if (accepted.isEmpty()) {
            pendingPanel.add(new Paragraph("✅ Todo asignado"));
            return;
        }

        for (Submission s : accepted) {
            pendingPanel.add(buildDraggableCard(s));
        }
    }

    private Div buildDraggableCard(Submission submission) {
        Div card = new Div();
        card.addClassNames(
                LumoUtility.Padding.SMALL,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Margin.Vertical.XSMALL,
                LumoUtility.Background.CONTRAST_5);
        card.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)").set("cursor", "grab");
        card.add(new Span(submission.getTitle()));
        card.add(
                new Paragraph("Speaker: " + submission.getSpeakerId().toString().substring(0, 8)));

        DragSource<Div> dragSource = DragSource.create(card);
        dragSource.setEffectAllowed(EffectAllowed.MOVE);
        dragSource.setDragData(submission.getId());

        return card;
    }

    private void renderTracks() {
        tracksPanel.removeAll();

        if (event.getTracks().isEmpty()) {
            tracksPanel.add(new Paragraph("Este evento no tiene tracks. Crea uno primero."));
            return;
        }

        for (EventTrack track : event.getTracks()) {
            tracksPanel.add(buildTrackColumn(track));
        }
    }

    private VerticalLayout buildTrackColumn(EventTrack track) {
        VerticalLayout column = new VerticalLayout();
        column.setSpacing(false);
        column.setPadding(true);
        column.setMinWidth("260px");
        column.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background", "var(--lumo-contrast-5pct)");

        column.add(new H3(track.getName()));

        // Slots existentes en este track
        var slots = agendaService.listForEvent(event.getId()).stream()
                .filter(s -> s.trackId() != null && s.trackId().equals(track.getId()))
                .sorted(Comparator.comparing(AgendaSlotSummary::startsAt))
                .toList();

        for (AgendaSlotSummary slot : slots) {
            column.add(new AgendaSlotCard(slot, userQueryService, () -> {
                agendaService.deleteSlot(slot.id());
                refresh();
            }));
        }

        // Drop target del track entero
        DropTarget<VerticalLayout> dropTarget = DropTarget.create(column);
        dropTarget.setDropEffect(DropEffect.MOVE);
        dropTarget.addDropListener(event -> event.getDragData().ifPresent(data -> {
            UUID submissionId = (UUID) data;
            assignSubmissionToTrack(submissionId, track);
        }));

        return column;
    }

    private void assignSubmissionToTrack(UUID submissionId, EventTrack track) {
        // Calcular siguiente hora disponible en ese track
        var existingInTrack = agendaService.listForEvent(event.getId()).stream()
                .filter(s -> s.trackId() != null && s.trackId().equals(track.getId()))
                .sorted(Comparator.comparing(AgendaSlotSummary::startsAt))
                .toList();

        Instant nextStart;
        if (existingInTrack.isEmpty()) {
            // Empezar a las 19:00 UTC del día del evento por default
            nextStart = event.getEventDate();
        } else {
            // 1 minuto después del último slot del track
            nextStart = existingInTrack.get(existingInTrack.size() - 1).endsAt().plus(Duration.ofMinutes(1));
        }

        try {
            agendaService.saveSlot(
                    event.getId(),
                    submissionId,
                    track.getId(),
                    nextStart,
                    nextStart.plus(Duration.ofMinutes(30)),
                    null);
            Notification.show("Slot asignado", 2000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refresh();
        } catch (AgendaConflictException ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void publish() {
        try {
            agendaService.publishEvent(event.getId());
            Notification.show("¡Evento publicado!", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            event = eventService.findById(event.getId()).orElseThrow();
        } catch (AgendaIncompleteException ex) {
            Notification.show(ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
