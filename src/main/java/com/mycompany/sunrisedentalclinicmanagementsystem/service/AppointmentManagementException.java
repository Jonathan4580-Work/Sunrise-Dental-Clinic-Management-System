package com.mycompany.sunrisedentalclinicmanagementsystem.service;

/**
 * Reports a categorized, user-safe appointment-management failure.
 */
public final class AppointmentManagementException extends Exception {

    public enum Reason {
        VALIDATION,
        NOT_FOUND,
        ALREADY_CANCELLED,
        INACTIVE_DENTIST,
        INACTIVE_TREATMENT,
        DENTIST_SCHEDULE_CONFLICT,
        PATIENT_SCHEDULE_CONFLICT,
        DATABASE_FAILURE
    }

    private final Reason reason;

    public AppointmentManagementException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AppointmentManagementException(
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
