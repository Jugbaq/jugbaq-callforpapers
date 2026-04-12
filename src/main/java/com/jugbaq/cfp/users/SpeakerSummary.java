package com.jugbaq.cfp.users;

import com.jugbaq.cfp.users.domain.User;
import java.util.UUID;

public record SpeakerSummary(UUID id, String fullName, String email) {
    // Método de fábrica para mapear fácilmente desde la entidad
    public static SpeakerSummary from(User user) {
        if (user == null) return null;
        return new SpeakerSummary(user.getId(), user.getFullName(), user.getEmail());
    }
}
