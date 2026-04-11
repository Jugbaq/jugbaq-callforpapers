package com.jugbaq.cfp.submissions.domain;

public class CfpClosedException extends RuntimeException {
    public CfpClosedException(String eventName) {
        super("El CFP para el evento '" + eventName + "' no está abierto");
    }
}
