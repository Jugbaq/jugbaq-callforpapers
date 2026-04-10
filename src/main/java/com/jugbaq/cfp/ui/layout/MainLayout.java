package com.jugbaq.cfp.ui.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
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

@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("CallForPapers");
        logo.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.MEDIUM
        );
        logo.getStyle().set("color", "var(--lumo-primary-color)");

        Span tenantBadge = new Span("JUGBAQ");
        tenantBadge.getElement().getThemeList().add("badge");

        HorizontalLayout header = new HorizontalLayout(
                new DrawerToggle(), logo, tenantBadge
        );
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Padding.Vertical.NONE,
                LumoUtility.Padding.Horizontal.MEDIUM
        );

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();

        // Sección pública
        nav.addItem(new SideNavItem("Eventos", "/t/jugbaq/events"));
        nav.addItem(new SideNavItem("Speakers", "/t/jugbaq/speakers"));

        // Sección speaker (se ocultará después según rol)
        H2 speakerSection = new H2("Speaker");
        speakerSection.addClassNames(
                LumoUtility.FontSize.XSMALL,
                LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.Margin.Bottom.XSMALL,
                LumoUtility.Padding.Horizontal.MEDIUM
        );
        speakerSection.getStyle().set("color", "var(--lumo-secondary-text-color)");

        SideNav speakerNav = new SideNav();
        speakerNav.addItem(new SideNavItem("Mis propuestas", "/t/jugbaq/my-submissions"));
        speakerNav.addItem(new SideNavItem("Mi perfil", "/t/jugbaq/my-profile"));

        // Sección organizador (se ocultará después según rol)
        H2 organizerSection = new H2("Organizador");
        organizerSection.addClassNames(
                LumoUtility.FontSize.XSMALL,
                LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.Margin.Bottom.XSMALL,
                LumoUtility.Padding.Horizontal.MEDIUM
        );
        organizerSection.getStyle().set("color", "var(--lumo-secondary-text-color)");

        SideNav adminNav = new SideNav();
        adminNav.addItem(new SideNavItem("Gestionar eventos", "/t/jugbaq/admin/events"));
        adminNav.addItem(new SideNavItem("Revisar propuestas", "/t/jugbaq/admin/reviews"));

        VerticalLayout drawerContent = new VerticalLayout(
                nav,
                speakerSection, speakerNav,
                organizerSection, adminNav
        );
        drawerContent.setSizeFull();
        drawerContent.setPadding(false);
        drawerContent.setSpacing(false);

        addToDrawer(drawerContent);
    }
}
