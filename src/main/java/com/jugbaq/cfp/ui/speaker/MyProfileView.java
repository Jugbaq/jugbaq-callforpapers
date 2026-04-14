package com.jugbaq.cfp.ui.speaker;

import com.jugbaq.cfp.shared.ratelimit.RateLimitService;
import com.jugbaq.cfp.ui.layout.MainLayout;
import com.jugbaq.cfp.users.SpeakerSummary;
import com.jugbaq.cfp.users.profile.AvatarStorageService;
import com.jugbaq.cfp.users.profile.SpeakerProfileService;
import com.jugbaq.cfp.users.profile.SpeakerProfileService.ProfileUpdateData;
import com.jugbaq.cfp.users.profile.SpeakerProfileService.SocialLinkData;
import com.jugbaq.cfp.users.security.SecurityUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Route(value = "t/:tenantSlug/my-profile", layout = MainLayout.class)
@PageTitle("Mi perfil")
@RolesAllowed("SPEAKER")
public class MyProfileView extends VerticalLayout {

    private static final List<String> PLATFORMS =
            List.of("TWITTER", "LINKEDIN", "GITHUB", "MASTODON", "BLUESKY", "YOUTUBE", "WEBSITE");

    private final SpeakerProfileService profileService;
    private final AvatarStorageService avatarStorage;
    private final SecurityUtils securityUtils;
    private final RateLimitService rateLimitService;

    private final Image avatarImg = new Image();
    private final TextField taglineField = new TextField("Tagline");
    private final TextArea bioField = new TextArea("Bio");
    private final TextField companyField = new TextField("Empresa");
    private final TextField jobTitleField = new TextField("Cargo");
    private final TextField cityField = new TextField("Ciudad");
    private final TextField countryField = new TextField("País");
    private final TextField websiteField = new TextField("Sitio web");
    private final VerticalLayout socialLinksContainer = new VerticalLayout();

    public MyProfileView(
            SpeakerProfileService profileService,
            AvatarStorageService avatarStorage,
            SecurityUtils securityUtils,
            RateLimitService rateLimitService) {
        this.profileService = profileService;
        this.avatarStorage = avatarStorage;
        this.securityUtils = securityUtils;
        this.rateLimitService = rateLimitService;

        setMaxWidth("720px");
        getStyle().set("margin", "0 auto");
        setPadding(true);

        add(new H2("Mi perfil"));

        buildAvatarSection();
        buildProfileForm();
        buildSocialLinksSection();

        Button saveBtn = new Button("Guardar cambios", e -> saveProfile());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(saveBtn);

        loadCurrentProfile();
    }

    private void buildAvatarSection() {
        avatarImg.setWidth("150px");
        avatarImg.setHeight("150px");
        avatarImg
                .getStyle()
                .set("border-radius", "50%")
                .set("object-fit", "cover")
                .set("border", "2px solid var(--lumo-contrast-20pct)");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(2 * 1024 * 1024); // 2 MB
        upload.setDropLabel(new Span("Arrastra una foto o haz click"));

        upload.addSucceededListener(event -> {
            UUID userId = securityUtils.getCurrentUserId().orElseThrow();
            if (!rateLimitService.uploadBucket(userId.toString()).tryConsume(1)) {
                showError("Demasiadas subidas. Intenta más tarde.");
                return;
            }
            try {
                String relativePath = avatarStorage.save(userId, event.getFileName(), buffer.getInputStream());
                profileService.setAvatar(userId, relativePath);
                avatarImg.setSrc(relativePath + "?t=" + System.currentTimeMillis());
                showSuccess("Foto actualizada");
            } catch (IOException | IllegalArgumentException ex) {
                showError("Error subiendo foto: " + ex.getMessage());
            }
        });

        HorizontalLayout avatarRow = new HorizontalLayout(avatarImg, upload);
        avatarRow.setAlignItems(Alignment.CENTER);
        add(avatarRow);
    }

    private void buildProfileForm() {
        taglineField.setMaxLength(200);
        taglineField.setHelperText("Una frase corta que te describa");

        bioField.setMaxLength(2000);
        bioField.setMinHeight("150px");
        bioField.setHelperText("Cuéntanos de ti");

        websiteField.setHelperText("https://...");

        FormLayout form = new FormLayout();
        form.add(taglineField, bioField, companyField, jobTitleField, cityField, countryField, websiteField);
        form.setColspan(taglineField, 2);
        form.setColspan(bioField, 2);
        form.setColspan(websiteField, 2);
        add(form);
    }

    private void buildSocialLinksSection() {
        add(new H3("Redes sociales"));

        socialLinksContainer.setPadding(false);
        socialLinksContainer.setSpacing(true);
        add(socialLinksContainer);

        Button addLinkBtn = new Button("+ Añadir red social", e -> socialLinksContainer.add(createLinkRow(null, null)));
        addLinkBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        add(addLinkBtn);
    }

    private HorizontalLayout createLinkRow(String platform, String url) {
        Select<String> platformSelect = new Select<>();
        platformSelect.setItems(PLATFORMS);
        platformSelect.setValue(platform != null ? platform : "TWITTER");
        platformSelect.setWidth("150px");

        TextField urlField = new TextField();
        urlField.setPlaceholder("https://...");
        urlField.setValue(url != null ? url : "");
        urlField.setWidthFull();

        Button removeBtn = new Button(new Icon(VaadinIcon.TRASH));
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

        HorizontalLayout row = new HorizontalLayout(platformSelect, urlField, removeBtn);
        row.setWidthFull();
        row.expand(urlField);

        removeBtn.addClickListener(e -> socialLinksContainer.remove(row));
        return row;
    }

    private void loadCurrentProfile() {
        UUID userId = securityUtils.getCurrentUserId().orElseThrow();

        SpeakerSummary profile = profileService.getSpeakerSummary(userId);

        if (profile.photoUrl() != null) {
            avatarImg.setSrc(profile.photoUrl());
        } else {
            avatarImg.setSrc("https://api.dicebear.com/7.x/initials/svg?seed=" + profile.fullName());
        }

        taglineField.setValue(profile.tagline() != null ? profile.tagline() : "");
        bioField.setValue(profile.bio() != null ? profile.bio() : "");
        companyField.setValue(profile.company() != null ? profile.company() : "");
        jobTitleField.setValue(profile.jobTitle() != null ? profile.jobTitle() : "");
        cityField.setValue(profile.city() != null ? profile.city() : "");
        countryField.setValue(profile.country() != null ? profile.country() : "");
        websiteField.setValue(profile.websiteUrl() != null ? profile.websiteUrl() : "");

        socialLinksContainer.removeAll();
        if (profile.socialLinks() != null) {
            // El DTO devuelve un Map<String, String>, así que iteramos (platform, url)
            profile.socialLinks().forEach((platform, url) -> socialLinksContainer.add(createLinkRow(platform, url)));
        }
    }

    private void saveProfile() {
        UUID userId = securityUtils.getCurrentUserId().orElseThrow();

        ProfileUpdateData data = new ProfileUpdateData();
        data.setTagline(taglineField.getValue());
        data.setBio(bioField.getValue());
        data.setCompany(companyField.getValue());
        data.setJobTitle(jobTitleField.getValue());
        data.setCity(cityField.getValue());
        data.setCountry(countryField.getValue());
        data.setWebsiteUrl(websiteField.getValue());

        List<SocialLinkData> links = new ArrayList<>();
        socialLinksContainer.getChildren().forEach(row -> {
            if (row instanceof HorizontalLayout hl) {
                Select<String> select = (Select<String>) hl.getComponentAt(0);
                TextField urlField = (TextField) hl.getComponentAt(1);
                if (urlField.getValue() != null && !urlField.getValue().isBlank()) {
                    links.add(new SocialLinkData(select.getValue(), urlField.getValue()));
                }
            }
        });

        try {
            profileService.updateProfile(userId, data);
            profileService.replaceSocialLinks(userId, links);
            showSuccess("Perfil actualizado");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void showSuccess(String msg) {
        Notification.show(msg, 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String msg) {
        Notification.show(msg, 4000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
