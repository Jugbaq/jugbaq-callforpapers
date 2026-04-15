package com.jugbaq.cfp.ui.layout;

import com.jugbaq.cfp.notifications.NotificationService;
import com.jugbaq.cfp.shared.tenant.TenantContext;
import com.jugbaq.cfp.shared.tenant.TenantRouteHelper;
import com.jugbaq.cfp.users.security.CfpUserDetails;
import com.jugbaq.cfp.users.security.SecurityUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AnonymousAllowed
@JsModule("@vaadin/vaadin-lumo-styles/icons.js")
public class MainLayout extends AppLayout {

    private static final Logger log = LoggerFactory.getLogger(MainLayout.class);
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private static final String STYLE_COLOR = "color";

    public MainLayout(SecurityUtils securityUtils, NotificationService notificationService) {
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;

        setPrimarySection(Section.DRAWER);

        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.getStyle().set(STYLE_COLOR, "var(--lumo-secondary-text-color)");

        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        actionsLayout.addClassNames(LumoUtility.Gap.SMALL);

        Optional<CfpUserDetails> user = securityUtils.getAuthenticatedUser();
        if (user.isPresent()) {
            String fullName = user.get().getFullName();

            Button themeToggle = new Button(VaadinIcon.MOON_O.create(), click -> {
                var themeList = UI.getCurrent().getElement().getThemeList();
                if (themeList.contains(Lumo.DARK)) {
                    themeList.remove(Lumo.DARK);
                    click.getSource().setIcon(VaadinIcon.MOON_O.create());
                } else {
                    themeList.add(Lumo.DARK);
                    click.getSource().setIcon(VaadinIcon.SUN_O.create());
                }
            });
            themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            themeToggle.setTooltipText("Cambiar tema");

            long unread = 0;
            try {
                unread = notificationService.unreadCount(user.get().getUserId());
            } catch (Exception e) {
                log.warn(
                        "No se pudo obtener el conteo de notificaciones para el usuario {}: {}",
                        user.get().getUserId(),
                        e.getMessage());
            }

            Button notifBtn = new Button(VaadinIcon.BELL_O.create());
            notifBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            notifBtn.addClickListener(
                    e -> notifBtn.getUI().ifPresent(ui -> ui.navigate(TenantRouteHelper.tenantPath("notifications"))));

            Div notifWrapper = new Div(notifBtn);
            notifWrapper.getStyle().set("position", "relative");

            if (unread > 0) {
                Span badge = new Span(String.valueOf(unread));
                badge.getElement().getThemeList().add("badge error primary small pill");
                badge.getStyle()
                        .set("position", "absolute")
                        .set("top", "-4px")
                        .set("right", "-4px")
                        .set("padding", "0 4px");
                notifWrapper.add(badge);
            }

            Avatar avatar = new Avatar(fullName);
            avatar.setColorIndex(Math.abs(fullName.hashCode() % 7));
            avatar.addClassNames(LumoUtility.Margin.Left.SMALL);

            Span userName = new Span(fullName);
            userName.addClassNames(
                    LumoUtility.FontSize.SMALL,
                    LumoUtility.FontWeight.MEDIUM,
                    LumoUtility.Display.HIDDEN,
                    LumoUtility.Display.Breakpoint.Small.FLEX);

            Button logoutBtn = new Button(VaadinIcon.SIGN_OUT.create(), event -> securityUtils.logout());
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
            logoutBtn.setTooltipText("Cerrar sesión");

            actionsLayout.add(themeToggle, notifWrapper, avatar, userName, logoutBtn);
        } else {
            Anchor loginLink = new Anchor("/login", "Iniciar sesión");
            loginLink.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
            actionsLayout.add(loginLink);
        }

        Header header = new Header(toggle, actionsLayout);
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.Padding.End.MEDIUM,
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.BoxShadow.XSMALL);
        header.getStyle().set("background", "var(--lumo-base-color)");

        addToNavbar(false, header);
    }

    private void createDrawer() {

        H1 logo = new H1("CallForPapers");
        logo.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE, LumoUtility.FontWeight.BOLD);
        logo.getStyle().set(STYLE_COLOR, "var(--lumo-primary-color)");

        Span tenantBadge = new Span(TenantContext.getTenantSlug().orElse("CFP").toUpperCase());
        tenantBadge.getElement().getThemeList().add("badge primary");
        tenantBadge.addClassNames(LumoUtility.Margin.Left.SMALL, LumoUtility.FontSize.XXSMALL);

        HorizontalLayout brandLayout = new HorizontalLayout(logo, tenantBadge);
        brandLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        brandLayout.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Margin.Bottom.SMALL);
        brandLayout.setSpacing(false);

        VerticalLayout drawerContent = new VerticalLayout();
        drawerContent.setSizeFull();
        drawerContent.setPadding(false);
        drawerContent.setSpacing(false);

        SideNav publicNav = new SideNav();
        publicNav.addItem(new SideNavItem(
                "Eventos", TenantRouteHelper.absoluteTenantPath("events"), VaadinIcon.CALENDAR.create()));
        publicNav.addItem(new SideNavItem(
                "Speakers", TenantRouteHelper.absoluteTenantPath("speakers"), VaadinIcon.USERS.create()));

        drawerContent.add(createSectionHeader("Explorar"), publicNav);

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

        Scroller scroller = new Scroller(drawerContent);

        addToDrawer(brandLayout, scroller);
    }

    private H2 createSectionHeader(String text) {
        H2 header = new H2(text);
        header.addClassNames(
                LumoUtility.FontSize.XSMALL,
                LumoUtility.Margin.Top.LARGE,
                LumoUtility.Margin.Bottom.SMALL,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.TextTransform.UPPERCASE,
                LumoUtility.FontWeight.BOLD);
        header.getStyle().set(STYLE_COLOR, "var(--lumo-tertiary-text-color)");
        return header;
    }

    private SideNav createSpeakerNav() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem(
                "Mis propuestas",
                TenantRouteHelper.absoluteTenantPath("my-submissions"),
                VaadinIcon.MICROPHONE.create()));
        nav.addItem(new SideNavItem(
                "Mi perfil", TenantRouteHelper.absoluteTenantPath("my-profile"), VaadinIcon.USER_CARD.create()));
        return nav;
    }

    private SideNav createOrganizerNav() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem(
                "Gestionar eventos", TenantRouteHelper.absoluteTenantPath("admin/events"), VaadinIcon.COG.create()));
        nav.addItem(new SideNavItem(
                "Revisar propuestas",
                TenantRouteHelper.absoluteTenantPath("admin/reviews"),
                VaadinIcon.CHECK_SQUARE_O.create()));
        return nav;
    }
}
