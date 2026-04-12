package com.jugbaq.cfp.ui.speaker;

import com.jugbaq.cfp.submissions.SubmissionService;
import com.jugbaq.cfp.submissions.domain.Submission;
import com.jugbaq.cfp.submissions.domain.SubmissionStatus;
import com.jugbaq.cfp.ui.layout.MainLayout;
import com.jugbaq.cfp.users.security.SecurityUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.UUID;

@Route(value = "t/:tenantSlug/my-submissions", layout = MainLayout.class)
@PageTitle("Mis propuestas")
@RolesAllowed("SPEAKER")
public class MySubmissionsView extends VerticalLayout {

    private final SubmissionService submissionService;
    private final SecurityUtils securityUtils;
    private final Grid<Submission> grid = new Grid<>(Submission.class, false);

    public MySubmissionsView(SubmissionService submissionService, SecurityUtils securityUtils) {
        this.submissionService = submissionService;
        this.securityUtils = securityUtils;

        setSizeFull();
        setPadding(true);

        add(new H2("Mis propuestas"));
        add(new Paragraph("Aquí puedes ver y gestionar las propuestas que has enviado."));

        configureGrid();
        add(grid);

        refresh();
    }

    private void configureGrid() {
        grid.addColumn(Submission::getTitle)
                .setHeader("Título")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(s -> s.getEvent().getName()).setHeader("Evento").setAutoWidth(true);

        grid.addComponentColumn(this::buildStatusBadge).setHeader("Estado");

        grid.addColumn(s -> s.getFormat() != null ? s.getFormat().getName() : "—")
                .setHeader("Formato");

        grid.addColumn(s -> s.getSubmittedAt() != null
                        ? s.getSubmittedAt().toString().substring(0, 10)
                        : "Borrador")
                .setHeader("Enviado");

        grid.addComponentColumn(this::buildActions).setHeader("Acciones").setAutoWidth(true);

        grid.setSizeFull();
    }

    private Span buildStatusBadge(Submission submission) {
        Span badge = new Span(submission.getStatus().name());
        badge.getElement().getThemeList().add("badge");

        switch (submission.getStatus()) {
            case DRAFT -> badge.getElement().getThemeList().add("contrast");
            case SUBMITTED, UNDER_REVIEW -> badge.getElement().getThemeList().add("primary");
            case ACCEPTED, CONFIRMED -> badge.getElement().getThemeList().add("success");
            case REJECTED -> badge.getElement().getThemeList().add("error");
            case WITHDRAWN -> {} // default gray
        }
        return badge;
    }

    private HorizontalLayout buildActions(Submission submission) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        // Botón "Enviar" solo si está en DRAFT
        if (submission.getStatus() == SubmissionStatus.DRAFT) {
            Button submitBtn = new Button("Enviar", e -> markAsSubmitted(submission));
            submitBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            actions.add(submitBtn);
        }

        // Botón "Retirar" si no está en estado terminal
        if (!submission.getStatus().isTerminal() && submission.getStatus() != SubmissionStatus.UNDER_REVIEW) {
            Button withdrawBtn = new Button("Retirar", e -> confirmWithdraw(submission));
            withdrawBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            actions.add(withdrawBtn);
        }

        return actions;
    }

    private void markAsSubmitted(Submission submission) {
        UUID speakerId = securityUtils.getCurrentUserId().orElseThrow();
        try {
            submissionService.markAsSubmitted(submission.getId(), speakerId);
            Notification.show("Propuesta enviada", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refresh();
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void confirmWithdraw(Submission submission) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("¿Retirar esta propuesta?");
        dialog.setText("Vas a retirar '" + submission.getTitle() + "'. Esta acción no se puede deshacer.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Sí, retirar");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> withdraw(submission));
        dialog.open();
    }

    private void withdraw(Submission submission) {
        UUID speakerId = securityUtils.getCurrentUserId().orElseThrow();
        try {
            submissionService.withdraw(submission.getId(), speakerId);
            Notification.show("Propuesta retirada", 3000, Notification.Position.TOP_CENTER);
            refresh();
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refresh() {
        UUID speakerId = securityUtils.getCurrentUserId().orElseThrow();
        List<Submission> submissions = submissionService.listBySpeaker(speakerId);
        grid.setItems(submissions);
    }
}
