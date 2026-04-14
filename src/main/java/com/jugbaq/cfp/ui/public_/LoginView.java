package com.jugbaq.cfp.ui.public_;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;
import java.util.Map;

@Route("login")
@PageTitle("Call For Papers - Iniciar Sesión")
@AnonymousAllowed
@Uses(Icon.class)
public class LoginView extends HorizontalLayout implements BeforeEnterObserver {

    public LoginView() {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER);
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Un fondito sutil para que resalte la tarjeta blanca
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        add(createLoginLayout());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Map<String, List<String>> params =
                event.getLocation().getQueryParameters().getParameters();
        if (params.containsKey("error")) {
            Notification.show("Usuario o contraseña incorrectos", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private Component createLoginLayout() {
        Section section = new Section();
        section.getStyle().setWidth("400px").set("background", "var(--lumo-base-color)");

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

        H1 loginH1 = new H1("CallForPapers");
        loginH1.getStyle().setColor("inherit");
        loginH1.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.XXLARGE);

        Paragraph subtitle = new Paragraph("Bienvenido de vuelta, Speaker");
        subtitle.getStyle().set("margin", "0").set("opacity", "0.8");

        brand.add(loginH1, subtitle);

        // --- Cuerpo del formulario ---
        Div form = new Div();
        form.addClassNames(LumoUtility.Padding.LARGE, LumoUtility.Display.FLEX);
        form.addClassNames(LumoUtility.FlexDirection.COLUMN, LumoUtility.BoxSizing.BORDER);

        // Usamos TextField genérico porque Spring Security por defecto usa 'username'
        final TextField username = new TextField("Usuario / Correo Electrónico");
        username.setPrefixComponent(VaadinIcon.USER.create());
        username.setRequiredIndicatorVisible(true);

        final PasswordField password = new PasswordField("Contraseña");
        password.setPrefixComponent(VaadinIcon.PASSWORD.create());
        password.setRequiredIndicatorVisible(true);

        Button loginBtn = new Button("Iniciar Sesión");
        loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginBtn.setWidthFull();
        loginBtn.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        loginBtn.addClickListener(click -> {
            if (username.isEmpty() || password.isEmpty()) {
                Notification.show("Por favor completa todos los campos", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            // Envío del form a Spring Security
            loginBtn.getElement()
                    .executeJs(
                            "const form = document.createElement('form');" + "form.method = 'POST';"
                                    + "form.action = '/login';"
                                    + "const userInput = document.createElement('input');"
                                    + "userInput.type = 'hidden';"
                                    + "userInput.name = 'username';"
                                    + "userInput.value = $0;"
                                    + "form.appendChild(userInput);"
                                    + "const passInput = document.createElement('input');"
                                    + "passInput.type = 'hidden';"
                                    + "passInput.name = 'password';"
                                    + "passInput.value = $1;"
                                    + "form.appendChild(passInput);"
                                    + "document.body.appendChild(form);"
                                    + "form.submit();",
                            username.getValue(),
                            password.getValue());
        });

        // --- Divisor ---
        Div divider = createDivider("o continúa con");

        // --- Botones OAuth ---
        Div oauthContainer = new Div();
        oauthContainer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        oauthContainer.getStyle().set("gap", "var(--lumo-space-s)");

        // Fíjate en la ruta de la imagen, debe coincidir con donde las pusiste
        oauthContainer.add(
                createOAuthButton("Continuar con Google", "google", "/oauth2/authorization/google"),
                createOAuthButton("Continuar con GitHub", "github", "/oauth2/authorization/github"));
        // Button githubBtn = new Button("Continuar con GitHub", LineAwesomeIcon.GITHUB.create());

        // Link de registro
        HorizontalLayout signupRow = new HorizontalLayout();
        signupRow.setJustifyContentMode(JustifyContentMode.CENTER);
        signupRow.addClassNames(LumoUtility.Margin.Top.LARGE);

        Anchor signUpLink = new Anchor("/register", "¿No tienes cuenta? Regístrate como speaker");
        signUpLink.getStyle().set("font-size", "0.9rem");

        signupRow.add(signUpLink);

        form.add(username, password, loginBtn, divider, oauthContainer, signupRow);

        section.add(brand, form);
        return section;
    }

    private Component createOAuthButton(String label, String iconName, String href) {
        // RUTA DE IMAGEN: Asume que están en src/main/frontend/themes/callforpapers/images/
        // Vaadin sirve las imágenes del tema desde "themes/[nombre-tema]/"
        Image icon = new Image("themes/callforpapers/images/%s.png".formatted(iconName), label);
        icon.setMaxWidth("20px");
        icon.setMaxHeight("20px");

        Span text = new Span(label);
        text.getStyle().set("font-weight", "500").set("color", "var(--lumo-body-text-color)");

        NativeButton btn = new NativeButton();
        btn.getElement().appendChild(icon.getElement());
        btn.getElement().appendChild(text.getElement());
        btn.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("gap", "var(--lumo-space-m)")
                .set("background", "transparent")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("cursor", "pointer")
                .set("min-height", "40px")
                .set("width", "100%");

        btn.getElement().executeJs("this.addEventListener('click', function() { window.location.href = $0; });", href);

        return btn;
    }

    private static Div createDivider(String text) {
        Span label = new Span(text);
        label.getStyle()
                .set("font-size", "0.8rem")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("background", "var(--lumo-base-color)")
                .set("padding", "0 var(--lumo-space-s)");

        Div divider = new Div(label);
        divider.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "100%")
                .set("margin", "var(--lumo-space-l) 0")
                .set("position", "relative");

        // Las líneas del divisor usando CSS pseudo-elements simulados
        divider.getElement()
                .executeJs(
                        "this.insertAdjacentHTML('afterbegin', '<hr style=\"position:absolute; width:100%; z-index:-1; border:none; border-top:1px solid var(--lumo-contrast-20pct); margin:0\">');");

        return divider;
    }
}
