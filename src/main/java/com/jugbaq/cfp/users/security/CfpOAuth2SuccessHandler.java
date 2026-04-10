package com.jugbaq.cfp.users.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CfpOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        delegate.onAuthenticationSuccess(request, response, authentication);
    }
}
