package com.jugbaq.cfp.ui.public_;

import com.jugbaq.cfp.ui.layout.MainLayout;
import com.jugbaq.cfp.users.SpeakerSummary;
import com.jugbaq.cfp.users.UserQueryService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Map;
import java.util.UUID;
import org.vaadin.lineawesome.LineAwesomeIcon;

@Route(value = "t/:tenantSlug/speakers/:userId", layout = MainLayout.class)
@PageTitle("Speaker")
@AnonymousAllowed
public class SpeakerDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final UserQueryService userQueryService;

    public SpeakerDetailView(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
        setMaxWidth("720px");
        getStyle().set("margin", "0 auto");
        setPadding(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String userIdParam = event.getRouteParameters().get("userId").orElse(null);
        if (userIdParam == null) {
            add(new H1("Speaker no encontrado"));
            return;
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdParam);
        } catch (IllegalArgumentException ex) {
            add(new H1("ID inválido"));
            return;
        }

        // --- BUENA PRÁCTICA: Usamos el Servicio y el DTO ---
        SpeakerSummary speaker = userQueryService.getSpeakerInfo(userId);
        if (speaker == null) {
            add(new H1("Speaker no encontrado"));
            return;
        }

        renderProfile(speaker);
    }

    private void renderProfile(SpeakerSummary speaker) {
        Image avatar = new Image();
        avatar.setWidth("180px");
        avatar.setHeight("180px");
        avatar.getStyle()
                .set("border-radius", "50%")
                .set("object-fit", "cover")
                .set("border", "3px solid var(--lumo-primary-color)");

        avatar.setSrc(
                speaker.photoUrl() != null
                        ? speaker.photoUrl()
                        : "https://api.dicebear.com/7.x/initials/svg?seed=" + speaker.fullName());
        add(avatar);

        H1 name = new H1(speaker.fullName());
        name.getStyle().set("color", "var(--lumo-primary-color)");
        add(name);

        if (speaker.tagline() != null && !speaker.tagline().isBlank()) {
            Paragraph tagline = new Paragraph(speaker.tagline());
            tagline.getStyle().set("font-size", "var(--lumo-font-size-l)").set("font-style", "italic");
            add(tagline);
        }

        if (speaker.company() != null || speaker.jobTitle() != null) {
            StringBuilder role = new StringBuilder();
            if (speaker.jobTitle() != null) role.append(speaker.jobTitle());
            if (speaker.company() != null) {
                if (role.length() > 0) role.append(" @ ");
                role.append(speaker.company());
            }
            add(new Paragraph(role.toString()));
        }

        // --- UBICACIÓN CON ICONO NATIVO ---
        if (speaker.city() != null || speaker.country() != null) {
            Icon locIcon = VaadinIcon.MAP_MARKER.create();
            locIcon.setSize("16px");
            locIcon.getStyle().set("margin-right", "8px").set("color", "var(--lumo-secondary-text-color)");

            StringBuilder loc = new StringBuilder();
            if (speaker.city() != null) loc.append(speaker.city());
            if (speaker.country() != null) {
                if (speaker.city() != null) loc.append(", ");
                loc.append(speaker.country());
            }

            HorizontalLayout locLayout = new HorizontalLayout(locIcon, new Span(loc.toString()));
            locLayout.setAlignItems(Alignment.CENTER);
            add(locLayout);
        }

        if (speaker.bio() != null && !speaker.bio().isBlank()) {
            add(new H2("Bio"));
            Paragraph bio = new Paragraph(speaker.bio());
            bio.getStyle().set("white-space", "pre-wrap");
            add(bio);
        }

        // --- WEBSITE CON ICONO NATIVO ---
        if (speaker.websiteUrl() != null && !speaker.websiteUrl().isBlank()) {
            Icon webIcon = VaadinIcon.GLOBE.create();
            webIcon.setSize("16px");
            webIcon.getStyle().set("margin-right", "8px");

            Anchor webAnchor = new Anchor(speaker.websiteUrl(), "");
            webAnchor.add(webIcon, new Span(speaker.websiteUrl()));
            webAnchor.getElement().setAttribute("target", "_blank");
            add(webAnchor);
        }

        if (speaker.socialLinks() != null && !speaker.socialLinks().isEmpty()) {
            add(new H2("Redes"));
            for (Map.Entry<String, String> link : speaker.socialLinks().entrySet()) {

                // 1. Recibimos el componente ya creado (sin .create() al final)
                Component socialIcon = iconFor(link.getKey());

                // 2. Le damos tamaño y margen usando el estilo directamente
                socialIcon
                        .getElement()
                        .getStyle()
                        .set("width", "16px")
                        .set("height", "16px")
                        .set("margin-right", "8px");

                Anchor a = new Anchor(link.getValue(), "");
                a.add(socialIcon, new Span(link.getKey()));
                a.getElement().setAttribute("target", "_blank");
                a.addClassNames(
                        LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Margin.Vertical.XSMALL);
                add(a);
            }
        }
    }

    private Component iconFor(String platform) {
        return switch (platform.toUpperCase()) {
                // Usamos los nombres clásicos de Line Awesome
            case "TWITTER", "X" -> LineAwesomeIcon.TWITTER.create();
            case "LINKEDIN" -> LineAwesomeIcon.LINKEDIN.create();
            case "GITHUB" -> LineAwesomeIcon.GITHUB.create();
            case "YOUTUBE" -> LineAwesomeIcon.YOUTUBE.create();
                // Para redes más nuevas como Mastodon o Bluesky usamos un link genérico
                // (Line Awesome no las alcanzó a incluir antes de dejar de actualizarse)
            default -> LineAwesomeIcon.LINK_SOLID.create();
        };
    }
}
