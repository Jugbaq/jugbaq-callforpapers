package com.jugbaq.cfp.users.domain;

import com.jugbaq.cfp.shared.domain.BaseEntity;
import com.jugbaq.cfp.shared.domain.Tenant;
import com.jugbaq.cfp.users.TenantRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OAuthAccount> oauthAccounts = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserTenantRole> tenantRoles = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private SpeakerProfile speakerProfile;

    protected User() {} // JPA

    public User(String email, String fullName) {
        this.email = email;
        this.fullName = fullName;
    }

    // --- Métodos de dominio ---

    public void addOAuthAccount(String provider, String providerUserId) {
        OAuthAccount account = new OAuthAccount(this, provider, providerUserId);
        this.oauthAccounts.add(account);
    }

    public void assignRole(Tenant tenant, TenantRole role) {
        this.tenantRoles.add(new UserTenantRole(this, tenant, role));
    }

    public boolean hasRole(UUID tenantId, TenantRole role) {
        return tenantRoles.stream().anyMatch(utr -> utr.getTenant().getId().equals(tenantId) && utr.getRole() == role);
    }

    public void initSpeakerProfile() {
        if (this.speakerProfile == null) {
            this.speakerProfile = new SpeakerProfile(this);
        }
    }

    // --- Getters / Setters ---

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Set<OAuthAccount> getOauthAccounts() {
        return Collections.unmodifiableSet(oauthAccounts);
    }

    public Set<UserTenantRole> getTenantRoles() {
        return Collections.unmodifiableSet(tenantRoles);
    }

    public SpeakerProfile getSpeakerProfile() {
        return speakerProfile;
    }
}
