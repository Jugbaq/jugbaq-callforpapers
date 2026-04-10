package com.jugbaq.cfp.shared.tenant;

public class TenantNotResolvedException extends RuntimeException {
    public TenantNotResolvedException(String message) {
        super(message);
    }
}

