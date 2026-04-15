package com.jugbaq.cfp.ui.admin;

import com.jugbaq.cfp.review.ReviewService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.UUID;

class ScoreDialog extends Dialog {

    ScoreDialog(UUID submissionId, UUID reviewerId, ReviewService reviewService, Runnable onSaved) {
        setHeaderTitle("Calificar propuesta");

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
        reviewService.listReviewsForSubmission(submissionId).stream()
                .filter(r -> r.reviewerId().equals(reviewerId))
                .findFirst()
                .ifPresent(existing -> {
                    scoreField.setValue(existing.score());
                    commentField.setValue(existing.comment() != null ? existing.comment() : "");
                });

        VerticalLayout content = new VerticalLayout(scoreField, commentField);
        content.setPadding(false);
        add(content);

        Button save = new Button("Guardar", e -> {
            try {
                reviewService.submitOrUpdateScore(
                        submissionId, reviewerId, scoreField.getValue(), commentField.getValue());
                Notification.show("Review guardada", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                close();
                onSaved.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        getFooter().add(new Button("Cancelar", e -> close()), save);
    }
}
