package com.jugbaq.cfp.users.security;

import com.jugbaq.cfp.shared.domain.TenantRepository;
import com.jugbaq.cfp.shared.tenant.TenantContext;
import com.jugbaq.cfp.users.domain.TenantRole;
import com.jugbaq.cfp.users.domain.User;
import com.jugbaq.cfp.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class CfpOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CfpOAuth2UserService.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final CfpUserDetailsService userDetailsService;

    public CfpOAuth2UserService(UserRepository userRepository,
                                TenantRepository tenantRepository,
                                CfpUserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.userDetailsService = userDetailsService;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String provider = registrationId.toUpperCase(); // GOOGLE o GITHUB

        String email = extractEmail(oAuth2User, provider);
        String name = extractName(oAuth2User, provider);
        String providerUserId = extractProviderUserId(oAuth2User, provider);

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    "No se pudo obtener el email desde " + provider
            );
        }

        // Buscar usuario existente
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> createNewUser(email, name, provider, providerUserId));

        // Vincular cuenta OAuth si aún no existe
        boolean hasProvider = user.getOauthAccounts().stream()
                .anyMatch(oa -> oa.getProvider().equals(provider));
        if (!hasProvider) {
            user.addOAuthAccount(provider, providerUserId);
            log.info("Vinculada cuenta {} a usuario {}", provider, email);
        }

        user.setEmailVerified(true); // OAuth providers verifican el email
        userRepository.save(user);

        CfpUserDetails details = userDetailsService.toCfpUserDetails(user);
        return new CfpOAuth2User(details, oAuth2User.getAttributes());
    }

    private User createNewUser(String email, String name, String provider, String providerUserId) {
        log.info("Creando nuevo usuario desde {}: {}", provider, email);

        User user = new User(email, name != null ? name : email);
        user.setEmailVerified(true);
        user.addOAuthAccount(provider, providerUserId);
        user.initSpeakerProfile();

        // Asignar SPEAKER al tenant actual si hay uno en contexto
        TenantContext.getTenantId().ifPresent(tenantId ->
                tenantRepository.findById(tenantId).ifPresent(tenant ->
                        user.assignRole(tenant, TenantRole.SPEAKER)
                )
        );

        // Si no hay tenant en contexto, asignar a jugbaq por defecto
        if (user.getTenantRoles().isEmpty()) {
            tenantRepository.findBySlug("jugbaq").ifPresent(tenant ->
                    user.assignRole(tenant, TenantRole.SPEAKER)
            );
        }

        return userRepository.save(user);
    }

    private String extractEmail(OAuth2User user, String provider) {
        return switch (provider) {
            case "GOOGLE" -> user.getAttribute("email");
            case "GITHUB" -> {
                String email = user.getAttribute("email");
                // GitHub puede no enviar email si es privado
                yield email;
            }
            default -> null;
        };
    }

    private String extractName(OAuth2User user, String provider) {
        return switch (provider) {
            case "GOOGLE" -> user.getAttribute("name");
            case "GITHUB" -> {
                String name = user.getAttribute("name");
                yield name != null ? name : user.getAttribute("login");
            }
            default -> null;
        };
    }

    private String extractProviderUserId(OAuth2User user, String provider) {
        return switch (provider) {
            case "GOOGLE" -> user.getAttribute("sub");
            case "GITHUB" -> String.valueOf(user.getAttribute("id"));
            default -> "unknown";
        };
    }
}
