package com.jugbaq.cfp.ui.public_;

import com.jugbaq.cfp.shared.ratelimit.RateLimitService;
import com.jugbaq.cfp.users.UserRegistrationService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route("register")
@PageTitle("Call For Papers - Registro de Speaker")
@AnonymousAllowed
@Uses(Icon.class)
public class RegisterView extends HorizontalLayout {

    private static final Logger log = LoggerFactory.getLogger(RegisterView.class);
    private final UserRegistrationService registrationService;
    private final RateLimitService rateLimitService;

    // Inyectamos tu servicio que ya funciona perfecto
    public RegisterView(UserRegistrationService registrationService, RateLimitService rateLimitService) {
        this.registrationService = registrationService;
        this.rateLimitService = rateLimitService;

        addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER);
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Mismo fondo sutil del login
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        add(createRegisterLayout());
    }

    private Component createRegisterLayout() {
        Section section = new Section();
        section.getStyle()
                .setWidth("450px") // Un poquito más ancho que el login porque pide más datos
                .set("background", "var(--lumo-base-color)");

        section.addClassNames(LumoUtility.MaxWidth.FULL, LumoUtility.BoxSizing.BORDER, LumoUtility.Overflow.HIDDEN);
        section.addClassNames(
                LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.BorderRadius.LARGE);
        section.addClassNames(LumoUtility.BoxShadow.MEDIUM, LumoUtility.Margin.SMALL, LumoUtility.Height.AUTO);

        // --- Cabecera azul (brand) ---
        Div brand = new Div();
        brand.getStyle()
                .set("padding", "var(--lumo-space-xl) var(--lumo-space-l) var(--lumo-space-l) var(--lumo-space-l)");

        brand.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.JustifyContent.END);
        brand.addClassNames(LumoUtility.Background.PRIMARY, LumoUtility.TextColor.PRIMARY_CONTRAST);

        H1 titleH1 = new H1("Únete como Speaker");
        titleH1.getStyle().setColor("inherit");
        titleH1.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.XXLARGE);

        Paragraph subtitle = new Paragraph("Crea tu cuenta para enviar propuestas");
        subtitle.getStyle().set("margin", "0").set("opacity", "0.8");

        brand.add(titleH1, subtitle);

        // --- Cuerpo del formulario ---
        Div form = new Div();
        form.addClassNames(LumoUtility.Padding.LARGE, LumoUtility.Display.FLEX);
        form.addClassNames(LumoUtility.FlexDirection.COLUMN, LumoUtility.BoxSizing.BORDER);

        // Campos del formulario
        TextField fullNameField = new TextField("Nombre Completo");
        fullNameField.setPrefixComponent(VaadinIcon.USER.create());
        fullNameField.setRequiredIndicatorVisible(true);

        EmailField emailField = new EmailField("Correo Electrónico");
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setRequiredIndicatorVisible(true);

        PasswordField passwordField = new PasswordField("Contraseña");
        passwordField.setPrefixComponent(VaadinIcon.PASSWORD.create());
        passwordField.setRequiredIndicatorVisible(true);

        PasswordField confirmPasswordField = new PasswordField("Confirmar Contraseña");
        confirmPasswordField.setPrefixComponent(VaadinIcon.CHECK.create());
        confirmPasswordField.setRequiredIndicatorVisible(true);

        Button registerBtn = new Button("Crear Cuenta");
        registerBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerBtn.setWidthFull();
        registerBtn.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        // Lógica al hacer clic en registrar
        registerBtn.addClickListener(click -> {
            String ip = VaadinRequest.getCurrent().getRemoteAddr();
            if (!rateLimitService.registrationBucket(ip).tryConsume(1)) {
                Notification.show("Demasiados intentos. Espera unos minutos.", 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            String fullName = fullNameField.getValue().trim();
            String email = emailField.getValue().trim();
            String password = passwordField.getValue();
            String confirmPassword = confirmPasswordField.getValue();

            // 1. Validar campos vacíos
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showError("Por favor completa todos los campos.");
                return;
            }

            // 2. Validar que las contraseñas coincidan
            if (!password.equals(confirmPassword)) {
                showError("Las contraseñas no coinciden.");
                return;
            }

            // 3. Validar si el correo ya existe (Usando tu servicio)
            if (registrationService.emailExists(email)) {
                showError("Este correo ya está registrado. Intenta iniciar sesión.");
                return;
            }

            try {
                // 4. Crear el usuario en la BD!
                registrationService.registerSpeaker(email, fullName, password);

                // 5. Éxito y redirección
                Notification success = Notification.show(
                        "¡Cuenta creada con éxito! Ya puedes iniciar sesión.", 4000, Notification.Position.TOP_CENTER);
                success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                // Redirigimos al login
                getUI().ifPresent(ui -> ui.navigate(LoginView.class));

            } catch (Exception e) {
                showError("Hubo un error al crear la cuenta. Intenta nuevamente.");
                log.error("Error al registrar el usuario con email {}: {}", email, e.getMessage(), e);
            }
        });

        // Link de volver al login
        HorizontalLayout loginRow = new HorizontalLayout();
        loginRow.setJustifyContentMode(JustifyContentMode.CENTER);
        loginRow.addClassNames(LumoUtility.Margin.Top.LARGE);

        Anchor loginLink = new Anchor("/login", "¿Ya tienes cuenta? Inicia Sesión");
        loginLink.getStyle().set("font-size", "0.9rem");

        loginRow.add(loginLink);

        form.add(fullNameField, emailField, passwordField, confirmPasswordField, registerBtn, loginRow);

        section.add(brand, form);
        return section;
    }

    private void showError(String message) {
        Notification.show(message, 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
