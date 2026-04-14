package com.jugbaq.cfp.ui.security;

import com.jugbaq.cfp.ui.public_.LoginView;
import com.jugbaq.cfp.users.security.CfpOAuth2SuccessHandler;
import com.jugbaq.cfp.users.security.CfpOAuth2UserService;
import com.jugbaq.cfp.users.security.CfpUserDetailsService;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final CfpUserDetailsService userDetailsService;
    private final CfpOAuth2UserService oAuth2UserService;
    private final CfpOAuth2SuccessHandler oAuth2SuccessHandler;

    public SecurityConfiguration(
            CfpUserDetailsService userDetailsService,
            CfpOAuth2UserService oAuth2UserService,
            CfpOAuth2SuccessHandler oAuth2SuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.oAuth2UserService = oAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1. Rutas públicas seguras usando AntPathRequestMatcher (Spring Sec 6+)
        // Agregamos también /line-awesome/** para que los logos de las redes carguen en la pantalla de login sin estar
        // autenticado
        http.authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/t/*/api/events/*/calendar.ics",
                                "/avatars/**",
                                "/line-awesome/**",
                                "/error",
                                "/actuator/health" 
                        ).permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN"));

        // 2. Configuración de OAuth2 (Google / GitHub)
        http.oauth2Login(oauth2 -> oauth2.loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                .successHandler(oAuth2SuccessHandler));

        // 3. UserDetailsService para auth estándar (correo y contraseña)
        http.userDetailsService(userDetailsService);

        http.with(VaadinSecurityConfigurer.vaadin(), vaadin -> {
            vaadin.loginView(LoginView.class);
        });

        http.headers(headers -> headers
                // X-Frame-Options: DENY
                .frameOptions(frame -> frame.deny())
                // HSTS: 1 año, subdominios incluidos
                .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                // Content-Security-Policy
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; " + "script-src 'self' 'unsafe-inline' 'unsafe-eval'; "
                                + "style-src 'self' 'unsafe-inline' fonts.googleapis.com; "
                                + "font-src 'self' fonts.gstatic.com; "
                                + "img-src 'self' data: https://api.dicebear.com; "
                                + "connect-src 'self'; "
                                + "frame-ancestors 'none'"))
                // Referrer policy
                .referrerPolicy(ref -> ref.policy(
                        org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy
                                .SAME_ORIGIN)));

        return http.build();
    }
}
