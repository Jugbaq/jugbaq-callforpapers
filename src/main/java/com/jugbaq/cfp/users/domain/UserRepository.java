package com.jugbaq.cfp.users.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Query("""
        SELECT u FROM User u
        JOIN u.oauthAccounts oa
        WHERE oa.provider = :provider AND oa.providerUserId = :providerUserId
        """)
    Optional<User> findByOAuthProviderAndId(String provider, String providerUserId);

    boolean existsByEmail(String email);
}
