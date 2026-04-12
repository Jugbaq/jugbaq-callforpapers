package com.jugbaq.cfp.events.domain;

public enum EventStatus {
    DRAFT, // Organizador editando
    CFP_OPEN, // Recibiendo propuestas
    CFP_CLOSED, // Ya no se reciben
    REVIEW, // En revisión
    PUBLISHED, // Agenda publicada
    COMPLETED, // Ya ocurrió
    CANCELLED;

    public boolean canTransitionTo(EventStatus target) {
        return switch (this) {
            case DRAFT -> target == CFP_OPEN || target == CANCELLED;
            case CFP_OPEN -> target == CFP_CLOSED || target == CANCELLED;
            case CFP_CLOSED -> target == REVIEW || target == CFP_OPEN;
            case REVIEW -> target == PUBLISHED || target == CFP_CLOSED;
            case PUBLISHED -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
