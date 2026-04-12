package com.jugbaq.cfp.ui.speaker;

import com.jugbaq.cfp.events.EventService;
import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventSessionFormat;
import com.jugbaq.cfp.events.domain.EventTrack;
import com.jugbaq.cfp.submissions.SubmissionData;
import com.jugbaq.cfp.submissions.SubmissionService;
import com.jugbaq.cfp.submissions.domain.CfpClosedException;
import com.jugbaq.cfp.submissions.domain.SubmissionLevel;
import com.jugbaq.cfp.submissions.domain.SubmissionLimitExceededException;
import com.jugbaq.cfp.ui.layout.MainLayout;
import com.jugbaq.cfp.users.security.SecurityUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.UUID;

@Route(value = "t/:tenantSlug/submit/:eventSlug", layout = MainLayout.class)
@PageTitle("Enviar propuesta")
@RolesAllowed("SPEAKER")
public class SubmitProposalView extends VerticalLayout implements BeforeEnterObserver {

    private final EventService eventService;
    private final SubmissionService submissionService;
    private final SecurityUtils securityUtils;

    private Event event;
    private final Binder<SubmissionData> binder = new Binder<>(SubmissionData.class);

    private final H2 title = new H2();
    private final Paragraph eventInfo = new Paragraph();
    private final TextField titleField = new TextField("Título de la charla");
    private final TextArea abstractField = new TextArea("Abstract");
    private final TextArea pitchField = new TextArea("Pitch (mensaje privado a organizadores)");
    private final Select<SubmissionLevel> levelField = new Select<>();
    private final Select<EventSessionFormat> formatField = new Select<>();
    private final Select<EventTrack> trackField = new Select<>();
    private final MultiSelectComboBox<String> tagsField = new MultiSelectComboBox<>("Tags");
    private final Button saveDraftBtn = new Button("Guardar borrador");
    private final Button submitBtn = new Button("Enviar propuesta");

    public SubmitProposalView(
            EventService eventService, SubmissionService submissionService, SecurityUtils securityUtils) {
        this.eventService = eventService;
        this.submissionService = submissionService;
        this.securityUtils = securityUtils;

        setMaxWidth("720px");
        getStyle().set("margin", "0 auto");
        setPadding(true);

        configureFields();
        configureBinder();
        configureButtons();

        FormLayout form = new FormLayout();
        form.add(titleField, abstractField, pitchField, levelField, formatField, trackField, tagsField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("600px", 2));
        form.setColspan(titleField, 2);
        form.setColspan(abstractField, 2);
        form.setColspan(pitchField, 2);
        form.setColspan(tagsField, 2);

        HorizontalLayout actions = new HorizontalLayout(saveDraftBtn, submitBtn);
        actions.setSpacing(true);

        add(title, eventInfo, form, actions);
    }

    private void configureFields() {
        titleField.setRequired(true);
        titleField.setMaxLength(250);
        titleField.setWidthFull();

        abstractField.setRequired(true);
        abstractField.setMinHeight("160px");
        abstractField.setWidthFull();
        abstractField.setHelperText("Descripción pública que verán los asistentes");

        pitchField.setMinHeight("100px");
        pitchField.setWidthFull();
        pitchField.setHelperText("Opcional: por qué esta charla importa. Solo organizadores lo ven.");

        levelField.setLabel("Nivel");
        levelField.setItems(SubmissionLevel.values());
        levelField.setValue(SubmissionLevel.INTERMEDIATE);

        formatField.setLabel("Formato");
        formatField.setItemLabelGenerator(EventSessionFormat::getName);

        trackField.setLabel("Track");
        trackField.setItemLabelGenerator(EventTrack::getName);

        tagsField.setAllowCustomValue(true);
        tagsField.setItems(
                "java",
                "kotlin",
                "spring-boot",
                "vaadin",
                "microservices",
                "testing",
                "devops",
                "ai",
                "cloud",
                "architecture");
        tagsField.setHelperText("Elige o crea tags separando con Enter");
    }

    private void configureBinder() {
        binder.forField(titleField)
                .asRequired("El título es obligatorio")
                .withValidator(t -> t != null && t.length() >= 10, "Mínimo 10 caracteres")
                .bind(SubmissionData::getTitle, SubmissionData::setTitle);

        binder.forField(abstractField)
                .asRequired("El abstract es obligatorio")
                .withValidator(a -> a != null && a.length() >= 50, "Mínimo 50 caracteres — cuéntanos de qué trata")
                .bind(SubmissionData::getAbstractText, SubmissionData::setAbstractText);

        binder.forField(pitchField).bind(SubmissionData::getPitch, SubmissionData::setPitch);

        binder.forField(levelField)
                .asRequired("Selecciona un nivel")
                .bind(SubmissionData::getLevel, SubmissionData::setLevel);

        binder.forField(formatField)
                .asRequired("Selecciona un formato")
                .bind(
                        data -> findFormatById(data.getFormatId()),
                        (data, format) -> data.setFormatId(format != null ? format.getId() : null));

        binder.forField(trackField)
                .bind(
                        data -> findTrackById(data.getTrackId()),
                        (data, track) -> data.setTrackId(track != null ? track.getId() : null));

        binder.forField(tagsField).bind(SubmissionData::getTags, SubmissionData::setTags);
    }

    private void configureButtons() {
        saveDraftBtn.addClickListener(e -> handleSave(false));
        submitBtn.addClickListener(e -> handleSave(true));
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }

    private void handleSave(boolean submit) {
        SubmissionData data = new SubmissionData();
        try {
            binder.writeBean(data);
        } catch (ValidationException ex) {
            showError("Revisa los errores del formulario");
            return;
        }

        UUID speakerId = securityUtils.getCurrentUserId().orElse(null);
        if (speakerId == null) {
            showError("Debes iniciar sesión para enviar una propuesta");
            return;
        }

        try {
            if (submit) {
                submissionService.createAndSubmit(event.getId(), speakerId, data);
                showSuccess("¡Propuesta enviada! Los organizadores la revisarán pronto.");
            } else {
                submissionService.createDraft(event.getId(), speakerId, data);
                showSuccess("Borrador guardado. Puedes editarlo más tarde.");
            }
            getUI().ifPresent(ui -> ui.navigate("t/jugbaq/my-submissions"));
        } catch (CfpClosedException ex) {
            showError("El CFP de este evento ya no está abierto");
        } catch (SubmissionLimitExceededException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Error al guardar: " + ex.getMessage());
        }
    }

    private EventSessionFormat findFormatById(UUID id) {
        if (id == null || event == null) return null;
        return event.getFormats().stream()
                .filter(f -> f.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private EventTrack findTrackById(UUID id) {
        if (id == null || event == null) return null;
        return event.getTracks().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private void showSuccess(String msg) {
        Notification n = Notification.show(msg, 4000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String msg) {
        Notification n = Notification.show(msg, 4000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        String eventSlug =
                beforeEnterEvent.getRouteParameters().get("eventSlug").orElse(null);
        if (eventSlug == null) {
            beforeEnterEvent.rerouteTo("t/jugbaq/events");
            return;
        }

        event = eventService.findBySlugWithDetails(eventSlug).orElse(null);

        if (event == null) {
            title.setText("Evento no encontrado");
            eventInfo.setText("El evento '" + eventSlug + "' no existe.");
            disableForm();
            return;
        }

        if (!event.isCfpOpen()) {
            title.setText("CFP cerrado");
            eventInfo.setText("El evento '" + event.getName() + "' no está recibiendo propuestas.");
            disableForm();
            return;
        }

        UUID speakerId = securityUtils.getCurrentUserId().orElse(null);

        int remainingSubmissions = event.getMaxSubmissionsPerSpeaker();

        if (speakerId != null) {
            // Contamos las propuestas del speaker para este evento que NO estén retiradas
            long activeSubmissions = submissionService.countActiveSubmissionsBySpeaker(event.getId(), speakerId);
            remainingSubmissions = event.getMaxSubmissionsPerSpeaker() - (int) activeSubmissions;
        }

        String remainingText =
                remainingSubmissions > 0 ? " (Te quedan " + remainingSubmissions + ")" : " (Has alcanzado el límite)";

        title.setText("Enviar propuesta — " + event.getName());
        eventInfo.setText("Fecha del evento: " + event.getEventDate() + " · Máximo "
                + event.getMaxSubmissionsPerSpeaker() + " propuestas por speaker" + remainingText);

        // Si ya alcanzó el límite, deshabilitamos el formulario de entrada
        if (remainingSubmissions <= 0) {
            disableForm();
            showError("Has alcanzado el límite máximo de propuestas para este evento.");
        }

        formatField.setItems(event.getFormats());
        if (!event.getFormats().isEmpty()) {
            formatField.setValue(event.getFormats().get(0));
        }

        trackField.setItems(event.getTracks());
        if (!event.getTracks().isEmpty()) {
            trackField.setValue(event.getTracks().get(0));
        }
    }

    private void disableForm() {
        titleField.setEnabled(false);
        abstractField.setEnabled(false);
        pitchField.setEnabled(false);
        levelField.setEnabled(false);
        formatField.setEnabled(false);
        trackField.setEnabled(false);
        tagsField.setEnabled(false);
        saveDraftBtn.setEnabled(false);
        submitBtn.setEnabled(false);
    }
}
