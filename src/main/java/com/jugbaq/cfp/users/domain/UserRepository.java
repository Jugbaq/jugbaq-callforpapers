package com.jugbaq.cfp.users.domain;

import com.jugbaq.cfp.users.TenantRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);

    @Query("""
        SELECT u FROM User u
        JOIN u.oauthAccounts oa
        WHERE oa.provider = :provider AND oa.providerUserId = :providerUserId
        """)
    Optional<User> findByOAuthProviderAndId(String provider, String providerUserId);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u JOIN u.tenantRoles tr WHERE tr.tenant.id = :tenantId AND tr.role IN (:roles)")
    List<User> findUsersByTenantAndRoles(
            @Param("tenantId") UUID tenantId,
            @Param("roles") List<TenantRole> roles
    );
}
