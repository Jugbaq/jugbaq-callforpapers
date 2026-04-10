package com.jugbaq.cfp.shared.config;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route("login")
@PageTitle("Login — CallForPapers")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H1 title = new H1("CallForPapers");
        title.getStyle().set("color", "var(--lumo-primary-color)");

        // Login form (email + password)
        loginForm.setAction("login"); // POST a /login que Spring Security maneja
        loginForm.setForgotPasswordButtonVisible(false);

        // Botones OAuth
        Div oauthSection = createOAuthSection();

        // Link de registro
        Paragraph registerLink = new Paragraph();
        Anchor registerAnchor = new Anchor("/register", "¿No tienes cuenta? Regístrate como speaker");
        registerLink.add(registerAnchor);

        add(title, loginForm, oauthSection, registerLink);
    }

    private Div createOAuthSection() {
        Div section = new Div();
        section.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL,
                LumoUtility.AlignItems.CENTER
        );

        Paragraph separator = new Paragraph("— o continúa con —");
        separator.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Anchor googleBtn = createOAuthButton(
                "/oauth2/authorization/google",
                "Continuar con Google"
        );

        Anchor githubBtn = createOAuthButton(
                "/oauth2/authorization/github",
                "Continuar con GitHub"
        );

        section.add(separator, googleBtn, githubBtn);
        return section;
    }

    private Anchor createOAuthButton(String href, String text) {
        Anchor anchor = new Anchor(href, text);
        anchor.getElement().setAttribute("router-ignore", true);
        anchor.getStyle()
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("min-width", "260px")
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("text-decoration", "none")
                .set("color", "var(--lumo-body-text-color)")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("cursor", "pointer");
        return anchor;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters()
                .containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
