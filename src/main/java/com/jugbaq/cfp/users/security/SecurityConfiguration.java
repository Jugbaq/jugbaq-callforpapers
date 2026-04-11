package com.jugbaq.cfp.users.security;

import com.jugbaq.cfp.users.ui.LoginView;
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

    public SecurityConfiguration(CfpUserDetailsService userDetailsService,
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

        // 1. Configuración de OAuth2 (Sin .permitAll())
        http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(oAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
        );

        // 2. Configuración de Form login (email + password) (Sin .permitAll())
        http.formLogin(form -> form
                .loginPage("/login")
        );

        // 3. Configuración de Logout
        http.logout(logout -> logout
                .logoutSuccessUrl("/login")
        );

        // 4. UserDetailsService para auth estándar
        http.userDetailsService(userDetailsService);

        // 5. Aplicamos las reglas de Vaadin (ESTO hace público el /login automáticamente)
        http.with(VaadinSecurityConfigurer.vaadin(), vaadin -> {
            vaadin.loginView(LoginView.class);
        });

        return http.build();
    }
}