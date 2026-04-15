package com.jugbaq.cfp.ui.admin;

import com.jugbaq.cfp.events.EventService;
import com.jugbaq.cfp.events.EventSummary;
import com.jugbaq.cfp.review.ReviewService;
import com.jugbaq.cfp.submissions.SubmissionService;
import com.jugbaq.cfp.submissions.SubmissionSummary;
import com.jugbaq.cfp.submissions.domain.SubmissionStatus;
import com.jugbaq.cfp.ui.layout.MainLayout;
import com.jugbaq.cfp.users.UserQueryService;
import com.jugbaq.cfp.users.security.SecurityUtils;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.UUID;

@Route(value = "t/:tenantSlug/admin/reviews", layout = MainLayout.class)
@PageTitle("Revisar propuestas")
@RolesAllowed({"ORGANIZER", "ADMIN"})
public class AdminReviewsView extends VerticalLayout {

    private final SubmissionService submissionService;
    private final ReviewService reviewService;
    private final EventService eventService;

    private final Select<EventSummary> eventFilter = new Select<>();
    private final Select<SubmissionStatus> statusFilter = new Select<>();
    private final Grid<SubmissionSummary> grid = new Grid<>(SubmissionSummary.class, false);
    private final SubmissionDetailPanel detailPanel;

    public AdminReviewsView(
            SubmissionService submissionService,
            ReviewService reviewService,
            EventService eventService,
            SecurityUtils securityUtils,
            UserQueryService userQueryService) {
        this.submissionService = submissionService;
        this.reviewService = reviewService;
        this.eventService = eventService;

        this.detailPanel = new SubmissionDetailPanel(reviewService, userQueryService, securityUtils);
        detailPanel.setActionCallback(() -> {
            refresh();
            detailPanel.clear();
        });

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        add(new H2("Revisar propuestas"));
        add(buildFilters());

        configureGrid();

        SplitLayout split = new SplitLayout(grid, detailPanel);
        split.setSplitterPosition(55);
        split.setSizeFull();
        addAndExpand(split);

        refresh();
    }

    private HorizontalLayout buildFilters() {
        eventFilter.setLabel("Evento");
        eventFilter.setItems(eventService.listAllSummaries());
        eventFilter.setItemLabelGenerator(e -> e == null ? "Todos" : e.name());
        eventFilter.setEmptySelectionAllowed(true);
        eventFilter.setEmptySelectionCaption("Todos los eventos");
        eventFilter.addValueChangeListener(e -> refresh());

        statusFilter.setLabel("Estado");
        statusFilter.setItems(
                SubmissionStatus.SUBMITTED,
                SubmissionStatus.UNDER_REVIEW,
                SubmissionStatus.ACCEPTED,
                SubmissionStatus.REJECTED);
        statusFilter.setItemLabelGenerator(s -> s == null ? "Todos" : s.name());
        statusFilter.setEmptySelectionAllowed(true);
        statusFilter.setEmptySelectionCaption("Todos");
        statusFilter.addValueChangeListener(e -> refresh());

        HorizontalLayout filters = new HorizontalLayout(eventFilter, statusFilter);
        filters.setPadding(false);
        return filters;
    }

    private void configureGrid() {
        grid.addColumn(SubmissionSummary::title).setHeader("Título").setFlexGrow(1);
        grid.addColumn(SubmissionSummary::eventName).setHeader("Evento").setAutoWidth(true);
        grid.addComponentColumn(this::buildStatusBadge).setHeader("Estado").setAutoWidth(true);
        grid.addColumn(s -> {
                    Double avg = reviewService.averageScore(s.id()).orElse(null);
                    return avg == null ? "—" : String.format("%.1f ⭐", avg);
                })
                .setHeader("Promedio")
                .setAutoWidth(true);

        grid.addSelectionListener(
                e -> e.getFirstSelectedItem().ifPresentOrElse(detailPanel::showSubmission, () -> detailPanel.clear()));
        grid.setSizeFull();
    }

    private Span buildStatusBadge(SubmissionSummary s) {
        Span badge = new Span(s.status().name());
        badge.getElement().getThemeList().add("badge");
        switch (s.status()) {
            case SUBMITTED, UNDER_REVIEW -> badge.getElement().getThemeList().add("primary");
            case ACCEPTED -> badge.getElement().getThemeList().add("success");
            case REJECTED -> badge.getElement().getThemeList().add("error");
            default -> {}
        }
        return badge;
    }

    private void refresh() {
        grid.deselectAll();
        UUID eventId = eventFilter.getValue() != null ? eventFilter.getValue().id() : null;
        SubmissionStatus status = statusFilter.getValue();
        grid.setItems(submissionService.listForReviewSummaries(eventId, status));
    }
}
