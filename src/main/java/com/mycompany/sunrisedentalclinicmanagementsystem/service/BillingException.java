package com.mycompany.sunrisedentalclinicmanagementsystem.service;

/**
 * Reports a categorized, user-safe billing failure.
 */
public final class BillingException extends Exception {

    public enum Reason {
        VALIDATION,
        APPOINTMENT_NOT_FOUND,
        CANCELLED_APPOINTMENT,
        INVALID_APPOINTMENT_STATUS,
        DUPLICATE_BILL,
        INACTIVE_TREATMENT,
        CONSULTATION_FEE_UNAVAILABLE,
        INACTIVE_USER,
        INVALID_AMOUNT,
        DATABASE_FAILURE
    }

    private final Reason reason;

    public BillingException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public BillingException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
