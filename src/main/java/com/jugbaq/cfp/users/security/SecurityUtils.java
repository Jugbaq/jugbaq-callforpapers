package com.jugbaq.cfp.users.security;

import com.vaadin.flow.server.VaadinServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SecurityUtils {

    /**
     * Retorna el CfpUserDetails del usuario autenticado.
     */
    public Optional<CfpUserDetails> getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof CfpUserDetails cfpUser) {
            return Optional.of(cfpUser);
        }

        if (principal instanceof CfpOAuth2User oauthUser) {
            return Optional.of(oauthUser.getUserDetails());
        }

        return Optional.empty();
    }

    public Optional<UUID> getCurrentUserId() {
        return getAuthenticatedUser().map(CfpUserDetails::getUserId);
    }

    public boolean isAuthenticated() {
        return getAuthenticatedUser().isPresent();
    }

    public void logout() {
        var logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(
                VaadinServletRequest.getCurrent().getHttpServletRequest(),
                null, null
        );
    }
}
