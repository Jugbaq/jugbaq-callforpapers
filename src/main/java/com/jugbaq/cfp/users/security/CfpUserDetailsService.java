package com.jugbaq.cfp.users.security;

import com.jugbaq.cfp.users.TenantRole;
import com.jugbaq.cfp.users.domain.User;
import com.jugbaq.cfp.users.domain.UserRepository;
import com.jugbaq.cfp.users.domain.UserStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CfpUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CfpUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + email
                ));

        return toCfpUserDetails(user);
    }

    /**
     * Construye CfpUserDetails a partir de una entidad User.
     * Reutilizado por el flujo OAuth2.
     */
    public CfpUserDetails toCfpUserDetails(User user) {
        Map<UUID, Set<TenantRole>> rolesByTenant = user.getTenantRoles().stream()
                .collect(Collectors.groupingBy(
                        utr -> utr.getTenant().getId(),
                        Collectors.mapping(
                                utr -> utr.getRole(),
                                Collectors.toSet()
                        )
                ));

        return new CfpUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getStatus() == UserStatus.ACTIVE,
                rolesByTenant
        );
    }
}
