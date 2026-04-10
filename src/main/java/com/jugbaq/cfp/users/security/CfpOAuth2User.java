package com.jugbaq.cfp.users.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CfpOAuth2User implements OAuth2User {

    private final CfpUserDetails userDetails;
    private final Map<String, Object> attributes;

    public CfpOAuth2User(CfpUserDetails userDetails, Map<String, Object> attributes) {
        this.userDetails = userDetails;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userDetails.getAuthorities();
    }

    @Override
    public String getName() { return userDetails.getEmail(); }

    public CfpUserDetails getUserDetails() { return userDetails; }
}
