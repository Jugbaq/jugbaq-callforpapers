package com.jugbaq.cfp.shared.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.formLogin(form -> form.permitAll());
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
                User.withDefaultPasswordEncoder()
                        .username("admin")
                        .password("admin")
                        .roles("ADMIN", "ORGANIZER")
                        .build(),
                User.withDefaultPasswordEncoder()
                        .username("speaker")
                        .password("speaker")
                        .roles("SPEAKER")
                        .build()
        );
    }
}
