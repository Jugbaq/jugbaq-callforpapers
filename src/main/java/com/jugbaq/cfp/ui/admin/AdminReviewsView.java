package com.jugbaq.cfp.ui.admin;

import com.jugbaq.cfp.events.EventService;
import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.review.DiscussionMessage;
import com.jugbaq.cfp.review.ReviewService;
import com.jugbaq.cfp.review.ReviewSummary;
import com.jugbaq.cfp.submissions.SubmissionService;
import com.jugbaq.cfp.submissions.domain.Submission;
import com.jugbaq.cfp.submissions.domain.SubmissionStatus;
import com.jugbaq.cfp.ui.layout.MainLayout;
import com.jugbaq.cfp.users.SpeakerSummary;
import com.jugbaq.cfp.users.UserQueryService;
import com.jugbaq.cfp.users.security.SecurityUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.UUID;

@Route(value = "t/:tenantSlug/admin/reviews", layout = MainLayout.class)
@PageTitle("Revisar propuestas")
@RolesAllowed({"ORGANIZER", "ADMIN"})
public class AdminReviewsView extends VerticalLayout {

    private final SubmissionService submissionService;
    private final ReviewService reviewService;
    private final EventService eventService;
    private final SecurityUtils securityUtils;

    private final Select<Event> eventFilter = new Select<>();
    private final Select<SubmissionStatus> statusFilter = new Select<>();
    private final Grid<Submission> grid = new Grid<>(Submission.class, false);
    private final VerticalLayout detailPanel = new VerticalLayout();

    private Submission selectedSubmission;

    private final UserQueryService userQueryService;

    public AdminReviewsView(
            SubmissionService submissionService,
            ReviewService reviewService,
            EventService eventService,
            SecurityUtils securityUtils,
            UserQueryService userQueryService) {
        this.submissionService = submissionService;
        this.reviewService = reviewService;
        this.eventService = eventService;
        this.securityUtils = securityUtils;
        this.userQueryService = userQueryService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        add(new H2("Revisar propuestas"));
        add(buildFilters());

        configureGrid();
        configureDetailPanel();

        SplitLayout split = new SplitLayout(grid, detailPanel);
        split.setSplitterPosition(55);
        split.setSizeFull();
        addAndExpand(split);

        refresh();
    }

    private HorizontalLayout buildFilters() {
        eventFilter.setLabel("Evento");
        eventFilter.setItems(eventService.listAll());
        eventFilter.setItemLabelGenerator(e -> e == null ? "Todos" : e.getName());
        eventFilter.setEmptySelectionAllowed(true);
        eventFilter.setEmptySelectionCaption("Todos los eventos");
        eventFilter.addValueChangeListener(e -> refresh());

        statusFilter.setLabel("Estado");
        statusFilter.setItems(
                SubmissionStatus.SUBMITTED,
                SubmissionStatus.UNDER_REVIEW,
                SubmissionStatus.ACCEPTED,
                SubmissionStatus.REJECTED);
        statusFilter.setItemLabelGenerator(s -> s == null ? "Todos" : s.name());
        statusFilter.setEmptySelectionAllowed(true);
        statusFilter.setEmptySelectionCaption("Todos");
        statusFilter.addValueChangeListener(e -> refresh());

        HorizontalLayout filters = new HorizontalLayout(eventFilter, statusFilter);
        filters.setPadding(false);
        return filters;
    }

    private void configureGrid() {
        grid.addColumn(Submission::getTitle).setHeader("Título").setFlexGrow(1);
        grid.addColumn(s -> s.getEvent().getName()).setHeader("Evento").setAutoWidth(true);
        grid.addComponentColumn(this::buildStatusBadge).setHeader("Estado").setAutoWidth(true);
        grid.addColumn(s -> {
                    Double avg = reviewService.averageScore(s.getId()).orElse(null);
                    return avg == null ? "—" : String.format("%.1f ⭐", avg);
                })
                .setHeader("Promedio")
                .setAutoWidth(true);

        grid.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(this::selectSubmission));
        grid.setSizeFull();
    }

    private Span buildStatusBadge(Submission s) {
        Span badge = new Span(s.getStatus().name());
        badge.getElement().getThemeList().add("badge");
        switch (s.getStatus()) {
            case SUBMITTED, UNDER_REVIEW -> badge.getElement().getThemeList().add("primary");
            case ACCEPTED -> badge.getElement().getThemeList().add("success");
            case REJECTED -> badge.getElement().getThemeList().add("error");
            default -> {}
        }
        return badge;
    }

    private void configureDetailPanel() {
        detailPanel.setPadding(true);
        detailPanel.setSpacing(true);
        detailPanel.setSizeFull();
        detailPanel.add(new Paragraph("Selecciona una propuesta para ver el detalle."));
    }

    private void selectSubmission(Submission submission) {
        this.selectedSubmission = submission;
        detailPanel.removeAll();

        detailPanel.add(new H3(submission.getTitle()));

        SpeakerSummary speaker = userQueryService.getSpeakerInfo(submission.getSpeakerId());
        String speakerName = speaker != null ? speaker.fullName() : "Usuario Desconocido";
        Paragraph speakerP = new Paragraph("Speaker: " + speakerName);
        speakerP.getStyle().set("font-weight", "500").set("color", "var(--lumo-secondary-text-color)");
        detailPanel.add(speakerP);

        // Abstract
        detailPanel.add(new H4("Abstract"));
        Paragraph abstractP = new Paragraph(submission.getAbstractText());
        abstractP.getStyle().set("white-space", "pre-wrap");
        detailPanel.add(abstractP);

        // Pitch (privado)
        if (submission.getPitch() != null && !submission.getPitch().isBlank()) {
            detailPanel.add(new H4("Pitch (privado)"));
            Paragraph pitchP = new Paragraph(submission.getPitch());
            pitchP.getStyle().set("white-space", "pre-wrap").set("color", "var(--lumo-secondary-text-color)");
            detailPanel.add(pitchP);
        }

        // Tags
        if (!submission.getTags().isEmpty()) {
            HorizontalLayout tagBar = new HorizontalLayout();
            tagBar.setSpacing(true);
            submission.getTags().forEach(tag -> {
                Span tagBadge = new Span(tag);
                tagBadge.getElement().getThemeList().add("badge");
                tagBar.add(tagBadge);
            });
            detailPanel.add(tagBar);
        }

        // Acciones principales
        detailPanel.add(buildActionBar(submission));

        // Reviews existentes
        detailPanel.add(new H4("Reviews"));
        renderReviews(submission);

        // Discusión
        detailPanel.add(new H4("Discusión interna"));
        renderDiscussion(submission);
    }

    private HorizontalLayout buildActionBar(Submission submission) {
        Button scoreBtn = new Button("Calificar", e -> openScoreDialog(submission));
        scoreBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button acceptBtn = new Button("Aceptar", e -> confirmAccept(submission));
        acceptBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        Button rejectBtn = new Button("Rechazar", e -> openRejectDialog(submission));
        rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        // Solo permitir accept/reject si está en estado revisable
        boolean canDecide = submission.getStatus() == SubmissionStatus.SUBMITTED
                || submission.getStatus() == SubmissionStatus.UNDER_REVIEW;
        acceptBtn.setEnabled(canDecide);
        rejectBtn.setEnabled(canDecide);
        scoreBtn.setEnabled(canDecide);

        HorizontalLayout bar = new HorizontalLayout(scoreBtn, acceptBtn, rejectBtn);
        bar.setSpacing(true);
        return bar;
    }

    private void renderReviews(Submission submission) {
        List<ReviewSummary> reviews = reviewService.listReviewsForSubmission(submission.getId());

        if (reviews.isEmpty()) {
            detailPanel.add(new Paragraph("Aún no hay reviews."));
            return;
        }

        Double avg = reviewService.averageScore(submission.getId()).orElse(null);
        if (avg != null) {
            Paragraph avgP = new Paragraph(String.format("Promedio: %.2f ⭐ (%d reviews)", avg, reviews.size()));
            avgP.getStyle().set("font-weight", "bold");
            detailPanel.add(avgP);
        }

        for (ReviewSummary r : reviews) {
            Div card = new Div();
            card.addClassNames(
                    LumoUtility.Padding.SMALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Vertical.XSMALL);
            card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
            card.add(new Span("⭐".repeat(r.score()) + " (" + r.score() + "/5)"));
            if (r.comment() != null && !r.comment().isBlank()) {
                Paragraph commentP = new Paragraph(r.comment());
                commentP.getStyle().set("margin", "4px 0 0 0").set("font-size", "var(--lumo-font-size-s)");
                card.add(commentP);
            }
            detailPanel.add(card);
        }
    }

    private void renderDiscussion(Submission submission) {
        List<DiscussionMessage> messages = reviewService.listDiscussion(submission.getId());

        VerticalLayout thread = new VerticalLayout();
        thread.setPadding(false);
        thread.setSpacing(false);

        if (messages.isEmpty()) {
            thread.add(new Paragraph("Sin mensajes todavía."));
        } else {
            for (DiscussionMessage msg : messages) {
                Div bubble = new Div();
                bubble.addClassNames(
                        LumoUtility.Padding.SMALL,
                        LumoUtility.BorderRadius.MEDIUM,
                        LumoUtility.Margin.Vertical.XSMALL,
                        LumoUtility.Background.CONTRAST_5);

                String authorName = userQueryService.getSpeakerFullName(msg.authorId());
                Span author = new Span(authorName);

                author.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-xs)");
                Paragraph body = new Paragraph(msg.message());
                body.getStyle().set("margin", "2px 0 0 0");
                bubble.add(author, body);
                thread.add(bubble);
            }
        }

        TextArea messageInput = new TextArea();
        messageInput.setPlaceholder("Escribe un mensaje al equipo...");
        messageInput.setWidthFull();
        messageInput.setMaxLength(1000);

        Button sendBtn = new Button("Enviar", e -> {
            String text = messageInput.getValue();
            if (text == null || text.isBlank()) return;
            UUID authorId = securityUtils.getCurrentUserId().orElseThrow();
            reviewService.postDiscussion(submission.getId(), authorId, text);
            messageInput.clear();
            selectSubmission(submission); // refresh
        });
        sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        detailPanel.add(thread, messageInput, sendBtn);
    }

    private void openScoreDialog(Submission submission) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Calificar propuesta");

        IntegerField scoreField = new IntegerField("Score (1-5)");
        scoreField.setMin(1);
        scoreField.setMax(5);
        scoreField.setStepButtonsVisible(true);
        scoreField.setValue(3);
        scoreField.setWidthFull();

        TextArea commentField = new TextArea("Comentario (interno)");
        commentField.setWidthFull();
        commentField.setMinHeight("120px");

        // Pre-cargar review existente del reviewer actual si la hay
        UUID reviewerId = securityUtils.getCurrentUserId().orElseThrow();
        reviewService.listReviewsForSubmission(submission.getId()).stream()
                .filter(r -> r.reviewerId().equals(reviewerId))
                .findFirst()
                .ifPresent(existing -> {
                    scoreField.setValue(existing.score());
                    commentField.setValue(existing.comment() != null ? existing.comment() : "");
                });

        VerticalLayout content = new VerticalLayout(scoreField, commentField);
        content.setPadding(false);
        dialog.add(content);

        Button save = new Button("Guardar", e -> {
            try {
                reviewService.submitOrUpdateScore(
                        submission.getId(), reviewerId, scoreField.getValue(), commentField.getValue());
                showSuccess("Review guardada");
                dialog.close();
                refresh();
                // Recargar la submission seleccionada
                submissionService.findById(submission.getId()).ifPresent(this::selectSubmission);
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(new Button("Cancelar", e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmAccept(Submission submission) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("¿Aceptar propuesta?");
        dialog.setText(
                "Vas a aceptar '" + submission.getTitle() + "'. Se enviará un email de confirmación al speaker.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Sí, aceptar");
        dialog.setConfirmButtonTheme("success primary");
        dialog.addConfirmListener(e -> {
            try {
                reviewService.accept(submission.getId());
                showSuccess("Propuesta aceptada. Email enviado al speaker.");
                refresh();
                submissionService.findById(submission.getId()).ifPresent(this::selectSubmission);
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        dialog.open();
    }

    private void openRejectDialog(Submission submission) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Rechazar propuesta");

        Paragraph info = new Paragraph("El feedback es opcional pero recomendado. Lo recibirá el speaker por email.");
        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        TextArea feedbackField = new TextArea("Feedback para el speaker (opcional)");
        feedbackField.setWidthFull();
        feedbackField.setMinHeight("120px");
        feedbackField.setPlaceholder("Ej: 'Tema interesante pero ya tenemos charlas similares en la agenda'");

        VerticalLayout content = new VerticalLayout(info, feedbackField);
        content.setPadding(false);
        dialog.add(content);

        Button confirm = new Button("Rechazar", e -> {
            try {
                reviewService.reject(submission.getId(), feedbackField.getValue());
                showSuccess("Propuesta rechazada. Email enviado al speaker.");
                dialog.close();
                refresh();
                submissionService.findById(submission.getId()).ifPresent(this::selectSubmission);
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(new Button("Cancelar", e -> dialog.close()), confirm);
        dialog.open();
    }

    private void refresh() {
        UUID eventId = eventFilter.getValue() != null ? eventFilter.getValue().getId() : null;
        SubmissionStatus status = statusFilter.getValue();
        grid.setItems(submissionService.listForReview(eventId, status));
    }

    private void showSuccess(String msg) {
        Notification.show(msg, 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String msg) {
        Notification.show(msg, 4000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
