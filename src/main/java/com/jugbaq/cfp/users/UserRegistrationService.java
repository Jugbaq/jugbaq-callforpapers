package com.jugbaq.cfp.users;

import com.jugbaq.cfp.shared.domain.TenantRepository;
import com.jugbaq.cfp.users.domain.User;
import com.jugbaq.cfp.users.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(
            UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerSpeaker(String email, String fullName, String rawPassword) {
        User user = new User(email, fullName);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEmailVerified(false);
        user.initSpeakerProfile();

        tenantRepository.findBySlug("jugbaq").ifPresent(tenant -> user.assignRole(tenant, TenantRole.SPEAKER));

        return userRepository.save(user);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }
}
