package com.jugbaq.cfp.ui.admin;

import com.jugbaq.cfp.review.DiscussionMessage;
import com.jugbaq.cfp.review.ReviewService;
import com.jugbaq.cfp.review.ReviewSummary;
import com.jugbaq.cfp.submissions.SubmissionSummary;
import com.jugbaq.cfp.submissions.domain.SubmissionStatus;
import com.jugbaq.cfp.users.SpeakerSummary;
import com.jugbaq.cfp.users.UserQueryService;
import com.jugbaq.cfp.users.security.SecurityUtils;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;
import java.util.UUID;

class SubmissionDetailPanel extends Composite<VerticalLayout> {

    @FunctionalInterface
    interface ReviewActionCallback {
        void onActionCompleted();
    }

    private final ReviewService reviewService;
    private final UserQueryService userQueryService;
    private final SecurityUtils securityUtils;

    private ReviewActionCallback actionCallback;

    SubmissionDetailPanel(ReviewService reviewService, UserQueryService userQueryService, SecurityUtils securityUtils) {
        this.reviewService = reviewService;
        this.userQueryService = userQueryService;
        this.securityUtils = securityUtils;

        getContent().setPadding(true);
        getContent().setSpacing(true);
        getContent().setSizeFull();
        getContent().add(new Paragraph("Selecciona una propuesta para ver el detalle."));
    }

    void setActionCallback(ReviewActionCallback callback) {
        this.actionCallback = callback;
    }

    void showSubmission(SubmissionSummary submission) {
        getContent().removeAll();

        getContent().add(new H3(submission.title()));

        SpeakerSummary speaker = userQueryService.getSpeakerInfo(submission.speakerId());
        String speakerName = speaker != null ? speaker.fullName() : "Usuario Desconocido";
        Paragraph speakerP = new Paragraph("Speaker: " + speakerName);
        speakerP.getStyle().set("font-weight", "500").set("color", "var(--lumo-secondary-text-color)");
        getContent().add(speakerP);

        // Abstract
        getContent().add(new H4("Abstract"));
        Paragraph abstractP = new Paragraph(submission.abstractText());
        abstractP.getStyle().set("white-space", "pre-wrap");
        getContent().add(abstractP);

        // Pitch (privado)
        if (submission.pitch() != null && !submission.pitch().isBlank()) {
            getContent().add(new H4("Pitch (privado)"));
            Paragraph pitchP = new Paragraph(submission.pitch());
            pitchP.getStyle().set("white-space", "pre-wrap").set("color", "var(--lumo-secondary-text-color)");
            getContent().add(pitchP);
        }

        // Tags
        if (!submission.tags().isEmpty()) {
            HorizontalLayout tagBar = new HorizontalLayout();
            tagBar.setSpacing(true);
            submission.tags().forEach(tag -> {
                Span tagBadge = new Span(tag);
                tagBadge.getElement().getThemeList().add("badge");
                tagBar.add(tagBadge);
            });
            getContent().add(tagBar);
        }

        // Acciones principales
        getContent().add(buildActionBar(submission));

        // Reviews existentes
        getContent().add(new H4("Reviews"));
        renderReviews(submission);

        // Discusión
        getContent().add(new H4("Discusión interna"));
        renderDiscussion(submission);
    }

    private HorizontalLayout buildActionBar(SubmissionSummary submission) {
        Runnable refreshCallback = () -> {
            if (actionCallback != null) actionCallback.onActionCompleted();
        };

        UUID reviewerId = securityUtils.getCurrentUserId().orElseThrow();

        Button scoreBtn = new Button(
                "Calificar", e -> new ScoreDialog(submission.id(), reviewerId, reviewService, refreshCallback).open());
        scoreBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button acceptBtn = new Button("Aceptar", e -> confirmAccept(submission, refreshCallback));
        acceptBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        Button rejectBtn =
                new Button("Rechazar", e -> new RejectDialog(submission.id(), reviewService, refreshCallback).open());
        rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        // Solo permitir accept/reject si está en estado revisable
        boolean canDecide = submission.status() == SubmissionStatus.SUBMITTED
                || submission.status() == SubmissionStatus.UNDER_REVIEW;
        acceptBtn.setEnabled(canDecide);
        rejectBtn.setEnabled(canDecide);
        scoreBtn.setEnabled(canDecide);

        HorizontalLayout bar = new HorizontalLayout(scoreBtn, acceptBtn, rejectBtn);
        bar.setSpacing(true);
        return bar;
    }

    private void confirmAccept(SubmissionSummary submission, Runnable onAccepted) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("¿Aceptar propuesta?");
        dialog.setText("Vas a aceptar '" + submission.title() + "'. Se enviará un email de confirmación al speaker.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Sí, aceptar");
        dialog.setConfirmButtonTheme("success primary");
        dialog.addConfirmListener(e -> {
            try {
                reviewService.accept(submission.id());
                Notification.show(
                                "Propuesta aceptada. Email enviado al speaker.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                onAccepted.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private void renderReviews(SubmissionSummary submission) {
        List<ReviewSummary> reviews = reviewService.listReviewsForSubmission(submission.id());

        if (reviews.isEmpty()) {
            getContent().add(new Paragraph("Aún no hay reviews."));
            return;
        }

        Double avg = reviewService.averageScore(submission.id()).orElse(null);
        if (avg != null) {
            Paragraph avgP = new Paragraph(String.format("Promedio: %.2f ⭐ (%d reviews)", avg, reviews.size()));
            avgP.getStyle().set("font-weight", "bold");
            getContent().add(avgP);
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
            getContent().add(card);
        }
    }

    private void renderDiscussion(SubmissionSummary submission) {
        List<DiscussionMessage> messages = reviewService.listDiscussion(submission.id());

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
            reviewService.postDiscussion(submission.id(), authorId, text);
            messageInput.clear();
            showSubmission(submission); // refresh
        });
        sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        getContent().add(thread, messageInput, sendBtn);
    }
}
