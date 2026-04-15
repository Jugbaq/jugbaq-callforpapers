package com.jugbaq.cfp.ui.public_;

import static com.jugbaq.cfp.shared.tenant.TenantRouteHelper.absoluteTenantPath;

import com.jugbaq.cfp.submissions.SubmissionService;
import com.jugbaq.cfp.ui.layout.MainLayout;
import com.jugbaq.cfp.users.SpeakerSummary;
import com.jugbaq.cfp.users.UserQueryService;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Route(value = "t/:tenantSlug/speakers", layout = MainLayout.class)
@PageTitle("Speakers")
@AnonymousAllowed
public class SpeakersListView extends VerticalLayout {

    public SpeakersListView(SubmissionService submissionService, UserQueryService userQueryService) {
        setMaxWidth("960px");
        getStyle().set("margin", "0 auto");
        setPadding(true);

        add(new H1("Speakers"));

        Set<UUID> speakerIds = submissionService.findAcceptedSpeakerIds();

        if (speakerIds.isEmpty()) {
            add(new Paragraph("Aún no hay speakers confirmados."));
            return;
        }

        List<SpeakerSummary> speakers = userQueryService.getSpeakersById(speakerIds);
        for (SpeakerSummary speaker : speakers) {
            add(buildSpeakerCard(speaker));
        }
    }

    private Div buildSpeakerCard(SpeakerSummary speaker) {
        Div card = new Div();
        card.addClassName("speaker-card");
        card.addClassNames(
                LumoUtility.Padding.MEDIUM,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Margin.Vertical.SMALL,
                // LumoUtility.Background.CONTRAST_5,
                LumoUtility.Display.FLEX,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.AlignItems.CENTER);

        // --- Avatar ---
        Image avatar = new Image();
        avatar.setWidth("80px");
        avatar.setHeight("80px");
        avatar.getStyle().set("border-radius", "50%").set("object-fit", "cover");

        String photoUrl = speaker.photoUrl() != null
                ? speaker.photoUrl()
                : "https://api.dicebear.com/7.x/initials/svg?seed=" + speaker.fullName();
        avatar.setSrc(photoUrl);

        // --- Info ---
        Div info = new Div();

        Anchor nameLink = new Anchor(absoluteTenantPath("speakers/" + speaker.id()), speaker.fullName());
        nameLink.getStyle().set("text-decoration", "none").set("color", "var(--lumo-header-text-color)");

        H3 nameHeader = new H3(nameLink);
        nameHeader.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.XSMALL);
        info.add(nameHeader);

        if (speaker.tagline() != null && !speaker.tagline().isBlank()) {
            Paragraph tag = new Paragraph(speaker.tagline());
            tag.getStyle().set("font-style", "italic");
            tag.addClassNames(LumoUtility.Margin.Vertical.NONE, LumoUtility.TextColor.SECONDARY);
            info.add(tag);
        }

        if (speaker.company() != null || speaker.jobTitle() != null) {
            String role = (speaker.jobTitle() != null ? speaker.jobTitle() : "")
                    + (speaker.company() != null ? " @ " + speaker.company() : "");
            Paragraph roleP = new Paragraph(role.trim());
            roleP.addClassNames(
                    LumoUtility.Margin.Vertical.NONE, LumoUtility.FontSize.SMALL, LumoUtility.TextColor.TERTIARY);
            info.add(roleP);
        }

        card.add(avatar, info);
        return card;
    }
}
