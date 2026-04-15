package com.jugbaq.cfp.ui.admin;

import com.jugbaq.cfp.publishing.AgendaSlotSummary;
import com.jugbaq.cfp.users.SpeakerSummary;
import com.jugbaq.cfp.users.UserQueryService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

class AgendaSlotCard extends Composite<Div> {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    AgendaSlotCard(AgendaSlotSummary slot, UserQueryService userQueryService, Runnable onRemove) {
        Div card = getContent();
        card.addClassNames(
                LumoUtility.Padding.SMALL,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Margin.Vertical.XSMALL,
                LumoUtility.Background.BASE,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.XSMALL);
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("border-left", "4px solid var(--lumo-primary-color)");

        // 1. La hora (pequeñita arriba)
        Span time = new Span(formatTime(slot.startsAt()) + " — " + formatTime(slot.endsAt()));
        time.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY, LumoUtility.FontWeight.BOLD);
        card.add(time);

        // 2. El Título (Resaltado)
        Paragraph title = new Paragraph(slot.displayTitle());
        title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.SEMIBOLD);
        title.getStyle().set("line-height", "1.2");
        card.add(title);

        // 3. El Nombre del Speaker con VaadinIcon nativo
        if (slot.speakerId() != null) {
            SpeakerSummary speaker = userQueryService.getSpeakerInfo(slot.speakerId());
            if (speaker != null) {
                Icon userIcon = VaadinIcon.USER.create();
                userIcon.setSize("12px");
                userIcon.getStyle().set("margin-right", "4px");

                Span speakerName = new Span(userIcon, new Span(speaker.fullName()));
                speakerName.addClassNames(
                        LumoUtility.Display.FLEX,
                        LumoUtility.AlignItems.CENTER,
                        LumoUtility.Margin.NONE,
                        LumoUtility.FontSize.XSMALL,
                        LumoUtility.TextColor.SECONDARY);

                card.add(speakerName);
            }
        }

        // 4. Botón de eliminar (Sutil abajo a la derecha)
        Button removeBtn = new Button("Remover", VaadinIcon.TRASH.create(), e -> onRemove.run());
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        removeBtn.getStyle().set("align-self", "flex-end").set("padding", "0");

        card.add(removeBtn);
    }

    private String formatTime(Instant instant) {
        return instant.atZone(ZONE).format(TIME_FORMAT);
    }
}
