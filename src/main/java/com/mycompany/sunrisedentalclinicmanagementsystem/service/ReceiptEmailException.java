package com.mycompany.sunrisedentalclinicmanagementsystem.service;

/**
 * Reports a categorized, user-safe receipt email failure.
 */
public final class ReceiptEmailException extends Exception {

    public enum Reason {
        VALIDATION,
        NOT_CONFIGURED,
        AUTHENTICATION_FAILURE,
        PROVIDER_REJECTED,
        NETWORK_FAILURE
    }

    private final Reason reason;

    public ReceiptEmailException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public ReceiptEmailException(
            Reason reason,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
