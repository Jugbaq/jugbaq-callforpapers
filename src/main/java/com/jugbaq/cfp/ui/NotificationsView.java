package com.jugbaq.cfp.ui;

import com.jugbaq.cfp.notifications.NotificationService;
import com.jugbaq.cfp.notifications.NotificationSummary;
import com.jugbaq.cfp.notifications.domain.Notification;
import com.jugbaq.cfp.ui.layout.MainLayout;
import com.jugbaq.cfp.users.security.SecurityUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.UUID;

@Route(value = "t/:tenantSlug/notifications", layout = MainLayout.class)
@PageTitle("Notificaciones")
@AnonymousAllowed
public class NotificationsView extends VerticalLayout {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;
    private final VerticalLayout list = new VerticalLayout();

    public NotificationsView(NotificationService notificationService, SecurityUtils securityUtils) {
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;

        setMaxWidth("720px");
        getStyle().set("margin", "0 auto");
        setPadding(true);

        HorizontalLayout header = new HorizontalLayout(new H2("Notificaciones"));
        Button markAllBtn = new Button("Marcar todas como leídas", e -> markAll());
        markAllBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        header.add(markAllBtn);
        header.setWidthFull();
        header.expand(header.getComponentAt(0));
        add(header);

        list.setPadding(false);
        list.setSpacing(false);
        add(list);

        refresh();
    }

    private void refresh() {
        list.removeAll();
        UUID userId = securityUtils.getCurrentUserId().orElse(null);
        if (userId == null) {
            list.add(new Paragraph("Inicia sesión para ver tus notificaciones."));
            return;
        }

        var notifications = notificationService.listForUser(userId);
        if (notifications.isEmpty()) {
            list.add(new Paragraph("No tienes notificaciones todavía."));
            return;
        }

        notifications.forEach(n -> list.add(buildItem(n)));
    }

    private Div buildItem(NotificationSummary n) {
        Div item = new Div();
        item.addClassNames(
                LumoUtility.Padding.MEDIUM,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Margin.Vertical.XSMALL
        );
        item.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        if (!n.isRead()) {
            item.getStyle().set("background", "var(--lumo-primary-color-10pct)");
        }

        String title = switch (n.type()) {
            case SUBMISSION_RECEIVED -> "Propuesta recibida: " + n.payload().get("title");
            case SUBMISSION_NEW_FOR_REVIEW -> "Nueva propuesta para revisar: " + n.payload().get("title");
            case SUBMISSION_ACCEPTED -> "Tu propuesta fue aceptada";
            case SUBMISSION_REJECTED -> "Propuesta no seleccionada";
        };

        item.add(new H3(title));
        item.add(new Paragraph(n.createdAt().toString().substring(0, 16).replace("T", " ")));

        if (!n.isRead()) {
            Button markBtn = new Button("Marcar como leída", e -> {
                notificationService.markAsRead(n.id());
                refresh();
            });
            markBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            item.add(markBtn);
        }

        return item;
    }

    private void markAll() {
        securityUtils.getCurrentUserId().ifPresent(id -> {
            notificationService.markAllAsRead(id);
            refresh();
        });
    }
}
