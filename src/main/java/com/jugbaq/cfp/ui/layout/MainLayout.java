package com.jugbaq.cfp.ui.layout;

import com.jugbaq.cfp.users.security.CfpUserDetails;
import com.jugbaq.cfp.users.security.SecurityUtils;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.Optional;

@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

    private final SecurityUtils securityUtils;

    public MainLayout(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("CallForPapers");
        logo.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);
        logo.getStyle().set("color", "var(--lumo-primary-color)");

        Span tenantBadge = new Span("JUGBAQ");
        tenantBadge.getElement().getThemeList().add("badge");

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, tenantBadge);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Padding.Vertical.NONE,
                LumoUtility.Padding.Horizontal.MEDIUM
        );

        // Auth section en el header
        Optional<CfpUserDetails> user = securityUtils.getAuthenticatedUser();
        if (user.isPresent()) {
            Span userName = new Span(user.get().getFullName());
            userName.addClassNames(LumoUtility.FontSize.SMALL);

            Button logoutBtn = new Button("Salir", event -> securityUtils.logout());
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            header.add(userName, logoutBtn);
        } else {
            Anchor loginLink = new Anchor("/login", "Iniciar sesión");
            header.add(loginLink);
        }

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav publicNav = new SideNav();
        publicNav.addItem(new SideNavItem("Eventos", "/t/jugbaq/events"));
        publicNav.addItem(new SideNavItem("Speakers", "/t/jugbaq/speakers"));

        VerticalLayout drawerContent = new VerticalLayout(publicNav);
        drawerContent.setSizeFull();
        drawerContent.setPadding(false);
        drawerContent.setSpacing(false);

        // Secciones según autenticación y roles
        Optional<CfpUserDetails> user = securityUtils.getAuthenticatedUser();
        if (user.isPresent()) {
            // Sección Speaker (todos los autenticados)
            drawerContent.add(
                    createSectionHeader("Speaker"),
                    createSpeakerNav()
            );

            // Sección Organizador (solo ORGANIZER y ADMIN)
            boolean isOrganizer = user.get().getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZER")
                            || a.getAuthority().equals("ROLE_ADMIN"));

            if (isOrganizer) {
                drawerContent.add(
                        createSectionHeader("Organizador"),
                        createOrganizerNav()
                );
            }
        }

        addToDrawer(drawerContent);
    }

    private H2 createSectionHeader(String text) {
        H2 header = new H2(text);
        header.addClassNames(
                LumoUtility.FontSize.XSMALL,
                LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.Margin.Bottom.XSMALL,
                LumoUtility.Padding.Horizontal.MEDIUM
        );
        header.getStyle().set("color", "var(--lumo-secondary-text-color)");
        return header;
    }

    private SideNav createSpeakerNav() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Mis propuestas", "/t/jugbaq/my-submissions"));
        nav.addItem(new SideNavItem("Mi perfil", "/t/jugbaq/my-profile"));
        return nav;
    }

    private SideNav createOrganizerNav() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Gestionar eventos", "/t/jugbaq/admin/events"));
        nav.addItem(new SideNavItem("Revisar propuestas", "/t/jugbaq/admin/reviews"));
        return nav;
    }
}
