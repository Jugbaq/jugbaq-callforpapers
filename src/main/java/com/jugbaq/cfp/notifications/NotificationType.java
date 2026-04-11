package com.jugbaq.cfp.notifications.domain;

public enum NotificationType {
    SUBMISSION_RECEIVED,      // Al speaker cuando manda propuesta
    SUBMISSION_NEW_FOR_REVIEW,// A los organizadores cuando llega una propuesta
    SUBMISSION_ACCEPTED,      // Al speaker cuando aceptan
    SUBMISSION_REJECTED       // Al speaker cuando rechazan 
}
