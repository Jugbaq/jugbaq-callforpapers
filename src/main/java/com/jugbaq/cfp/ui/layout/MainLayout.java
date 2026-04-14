package com.jugbaq.cfp.ui.layout;

import com.jugbaq.cfp.notifications.NotificationService;
import com.jugbaq.cfp.users.security.CfpUserDetails;
import com.jugbaq.cfp.users.security.SecurityUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Optional;

@AnonymousAllowed
public class MainLayout extends AppLayout {

    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;

    public MainLayout(SecurityUtils securityUtils, NotificationService notificationService) {
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;

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
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);

        // Auth section en el header
        Optional<CfpUserDetails> user = securityUtils.getAuthenticatedUser();
        if (user.isPresent()) {
            String fullName = user.get().getFullName();

            // --- 1. Botón de Modo Oscuro ---
            Button themeToggle = new Button(VaadinIcon.MOON_O.create(), click -> {
                var themeList = UI.getCurrent().getElement().getThemeList();
                if (themeList.contains(Lumo.DARK)) {
                    themeList.remove(Lumo.DARK);
                    click.getSource().setIcon(VaadinIcon.MOON_O.create()); // Pone la luna
                } else {
                    themeList.add(Lumo.DARK);
                    click.getSource().setIcon(VaadinIcon.SUN_O.create()); // Pone el sol
                }
            });
            themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            themeToggle.setTooltipText("Cambiar tema"); // Un tooltip coqueto

            // --- 2. Botón de Notificaciones ---
            long unread = 0;
            try {
                unread = notificationService.unreadCount(user.get().getUserId());
            } catch (Exception e) {
                e.printStackTrace();
            }
            Icon bellIcon = VaadinIcon.BELL.create();
            Button notifBtn = new Button(unread > 0 ? String.valueOf(unread) : "", bellIcon);
            notifBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            if (unread > 0) {
                notifBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            }
            notifBtn.addClickListener(e -> notifBtn.getUI().ifPresent(ui -> ui.navigate("t/jugbaq/notifications")));

            // --- 3. Avatar y Nombre (Paso 2) ---
            Avatar avatar = new Avatar(fullName);
            avatar.setColorIndex(Math.abs(fullName.hashCode() % 7));

            Span userName = new Span(fullName);
            // Solo le dejamos el tamaño y la fuente, y borramos la línea del HIDDEN
            userName.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.MEDIUM);

            HorizontalLayout profileInfo = new HorizontalLayout(avatar, userName);
            profileInfo.setAlignItems(FlexComponent.Alignment.CENTER);
            profileInfo.setSpacing(true);

            // --- 4. Botón de Salir ---
            Button logoutBtn = new Button("Salir", event -> securityUtils.logout());
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            // Añadimos todo al header en orden
            header.add(themeToggle, notifBtn, profileInfo, logoutBtn);
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

        Optional<CfpUserDetails> user = securityUtils.getAuthenticatedUser();
        if (user.isPresent()) {
            drawerContent.add(createSectionHeader("Speaker"), createSpeakerNav());

            boolean isOrganizer = user.get().getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZER")
                            || a.getAuthority().equals("ROLE_ADMIN"));

            if (isOrganizer) {
                drawerContent.add(createSectionHeader("Organizador"), createOrganizerNav());
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
                LumoUtility.Padding.Horizontal.MEDIUM);
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
