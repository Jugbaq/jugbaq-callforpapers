package com.jugbaq.cfp.users.domain;

import com.jugbaq.cfp.shared.domain.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_tenant_roles")
@IdClass(UserTenantRole.UserTenantRoleId.class)
public class UserTenantRole {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TenantRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserTenantRole() {} // JPA

    public UserTenantRole(User user, Tenant tenant, TenantRole role) {
        this.user = user;
        this.tenant = tenant;
        this.role = role;
        this.createdAt = Instant.now();
    }

    public User getUser() { return user; }
    public Tenant getTenant() { return tenant; }
    public TenantRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }

    // --- Composite ID class ---
    public static class UserTenantRoleId implements Serializable {
        private UUID user;
        private UUID tenant;
        private TenantRole role;

        public UserTenantRoleId() {}

        public UserTenantRoleId(UUID user, UUID tenant, TenantRole role) {
            this.user = user;
            this.tenant = tenant;
            this.role = role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UserTenantRoleId that)) return false;
            return Objects.equals(user, that.user)
                    && Objects.equals(tenant, that.tenant)
                    && role == that.role;
        }

        @Override
        public int hashCode() {
            return Objects.hash(user, tenant, role);
        }
    }
}
