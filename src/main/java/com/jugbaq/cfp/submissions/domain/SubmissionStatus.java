package com.jugbaq.cfp.submissions.domain;

public enum SubmissionStatus {
    DRAFT,           // Speaker editando, aún no envía
    SUBMITTED,       // Enviada, esperando review
    UNDER_REVIEW,    // Los organizadores están revisando
    ACCEPTED,        // Aceptada por los organizadores
    REJECTED,        // Rechazada
    WITHDRAWN,       // Speaker la retiró
    CONFIRMED;       // Speaker confirmó su presencia

    public boolean canTransitionTo(SubmissionStatus target) {
        return switch (this) {
            case DRAFT        -> target == SUBMITTED || target == WITHDRAWN;
            case SUBMITTED    -> target == UNDER_REVIEW || target == WITHDRAWN || target == DRAFT;
            case UNDER_REVIEW -> target == ACCEPTED || target == REJECTED;
            case ACCEPTED     -> target == CONFIRMED || target == WITHDRAWN;
            case REJECTED, CONFIRMED, WITHDRAWN -> false; // terminales
        };
    }

    public boolean isEditableBySpeaker() {
        return this == DRAFT || this == SUBMITTED;
    }

    public boolean isTerminal() {
        return this == REJECTED || this == CONFIRMED || this == WITHDRAWN;
    }
}
