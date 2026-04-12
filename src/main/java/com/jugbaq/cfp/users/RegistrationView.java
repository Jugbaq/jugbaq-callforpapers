package com.jugbaq.cfp.users;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("register")
@PageTitle("Registro — CallForPapers")
@AnonymousAllowed
public class RegistrationView extends VerticalLayout {

    public RegistrationView(UserRegistrationService userRegistrationService) {

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        setMaxWidth("400px");
        getStyle().set("margin", "0 auto");

        H2 title = new H2("Registro de Speaker");

        TextField fullNameField = new TextField("Nombre completo");
        fullNameField.setWidthFull();
        fullNameField.setRequired(true);
        fullNameField.setMinLength(2);
        fullNameField.setMaxLength(200);

        EmailField emailField = new EmailField("Email");
        emailField.setWidthFull();
        emailField.setRequired(true);

        PasswordField passwordField = new PasswordField("Contraseña");
        passwordField.setWidthFull();
        passwordField.setRequired(true);
        passwordField.setMinLength(8);
        passwordField.setHelperText("Mínimo 8 caracteres");

        PasswordField confirmPasswordField = new PasswordField("Confirmar contraseña");
        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequired(true);

        Button registerBtn = new Button("Registrarme");
        registerBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerBtn.setWidthFull();

        Paragraph loginLink = new Paragraph();
        loginLink.add(new Anchor("/login", "¿Ya tienes cuenta? Inicia sesión"));

        // Binder para validación
        Binder<RegistrationData> binder = new Binder<>(RegistrationData.class);

        binder.forField(fullNameField)
                .asRequired("El nombre es obligatorio")
                .bind(RegistrationData::getFullName, RegistrationData::setFullName);

        binder.forField(emailField)
                .asRequired("El email es obligatorio")
                .withValidator(new EmailValidator("Email inválido"))
                .withValidator(email -> !userRegistrationService.emailExists(email),
                        "Ya existe una cuenta con este email")
                .bind(RegistrationData::getEmail, RegistrationData::setEmail);

        binder.forField(passwordField)
                .asRequired("La contraseña es obligatoria")
                .withValidator(p -> p.length() >= 8, "Mínimo 8 caracteres")
                .bind(RegistrationData::getPassword, RegistrationData::setPassword);

        binder.forField(confirmPasswordField)
                .asRequired("Confirma tu contraseña")
                .withValidator(confirm -> confirm.equals(passwordField.getValue()),
                        "Las contraseñas no coinciden")
                .bind(RegistrationData::getConfirmPassword, RegistrationData::setConfirmPassword);

        registerBtn.addClickListener(event -> {
            RegistrationData data = new RegistrationData();
            try {
                binder.writeBean(data);

                userRegistrationService.registerSpeaker(data.getEmail(), data.getFullName(), data.getPassword());

                Notification.show(
                        "¡Registro exitoso! Ya puedes iniciar sesión.",
                        4000,
                        Notification.Position.TOP_CENTER
                ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                getUI().ifPresent(ui -> ui.navigate("login"));

            } catch (ValidationException e) {
                Notification.show(
                        "Corrige los errores del formulario",
                        3000,
                        Notification.Position.TOP_CENTER
                ).addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception e) {
            // Catch genérico por si falla algo en la base de datos durante el guardado
            Notification.show(
                    "Error al registrar el usuario. Inténtalo de nuevo.",
                    4000,
                    Notification.Position.TOP_CENTER
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
            e.printStackTrace(); // En producción, deberías usar un logger aquí
        }
        });

        add(title, fullNameField, emailField, passwordField, confirmPasswordField,
                registerBtn, loginLink);
    }

    /**
     * DTO interno para el binder del formulario de registro.
     */
    public static class RegistrationData {
        private String fullName;
        private String email;
        private String password;
        private String confirmPassword;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}
