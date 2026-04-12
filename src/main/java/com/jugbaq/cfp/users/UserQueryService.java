package com.jugbaq.cfp.users;

import com.jugbaq.cfp.users.domain.UserRepository;
import com.jugbaq.cfp.users.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;

    public UserQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getSpeakerFullName(UUID speakerId) {
        if (speakerId == null) {
            return "Usuario Desconocido";
        }
        return userRepository.findById(speakerId)
                .map(User::getFullName)
                .orElse("Speaker no encontrado");
    }

    public SpeakerSummary getSpeakerInfo(UUID speakerId) {
        return userRepository.findById(speakerId)
                .map(SpeakerSummary::from)
                .orElse(null);
    }

    public List<SpeakerSummary> findOrganizersAndAdmins(UUID tenantId) {
        List<TenantRole> rolesToFind = List.of(TenantRole.ORGANIZER, TenantRole.ADMIN);
        return userRepository.findUsersByTenantAndRoles(tenantId, rolesToFind).stream()
                .map(SpeakerSummary::from)
                .toList();
    }
}
