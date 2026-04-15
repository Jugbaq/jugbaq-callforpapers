package com.jugbaq.cfp.ui.admin;

import com.jugbaq.cfp.review.ReviewService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.UUID;

class RejectDialog extends Dialog {

    RejectDialog(UUID submissionId, ReviewService reviewService, Runnable onRejected) {
        setHeaderTitle("Rechazar propuesta");

        Paragraph info = new Paragraph("El feedback es opcional pero recomendado. Lo recibirá el speaker por email.");
        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        TextArea feedbackField = new TextArea("Feedback para el speaker (opcional)");
        feedbackField.setWidthFull();
        feedbackField.setMinHeight("120px");
        feedbackField.setPlaceholder("Ej: 'Tema interesante pero ya tenemos charlas similares en la agenda'");

        VerticalLayout content = new VerticalLayout(info, feedbackField);
        content.setPadding(false);
        add(content);

        Button confirm = new Button("Rechazar", e -> {
            try {
                reviewService.reject(submissionId, feedbackField.getValue());
                Notification.show(
                                "Propuesta rechazada. Email enviado al speaker.",
                                3000,
                                Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                close();
                onRejected.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        getFooter().add(new Button("Cancelar", e -> close()), confirm);
    }
}
