package com.jugbaq.cfp.users.security;

import com.jugbaq.cfp.users.domain.TenantRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

public class CfpUserDetails implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final boolean active;
    private final Map<UUID, Set<TenantRole>> rolesByTenant;

    public CfpUserDetails(UUID userId,
                          String email,
                          String passwordHash,
                          String fullName,
                          boolean active,
                          Map<UUID, Set<TenantRole>> rolesByTenant) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.active = active;
        this.rolesByTenant = rolesByTenant;
    }

    /**
     * Retorna authorities combinando roles de TODOS los tenants.
     * Para filtrar por tenant específico, usar getRolesForTenant().
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        rolesByTenant.values().stream()
                .flatMap(Set::stream)
                .distinct()
                .forEach(role -> authorities.add(
                        new SimpleGrantedAuthority("ROLE_" + role.name())
                ));
        return authorities;
    }

    public Set<TenantRole> getRolesForTenant(UUID tenantId) {
        return rolesByTenant.getOrDefault(tenantId, Set.of());
    }

    public boolean hasRoleInTenant(UUID tenantId, TenantRole role) {
        return getRolesForTenant(tenantId).contains(role);
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return active; }

    @Override
    public boolean isAccountNonLocked() { return active; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return active; }

    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public Map<UUID, Set<TenantRole>> getRolesByTenant() { return rolesByTenant; }
}
