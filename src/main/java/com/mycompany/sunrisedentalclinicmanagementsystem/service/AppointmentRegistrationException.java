package com.mycompany.sunrisedentalclinicmanagementsystem.service;

/**
 * Reports a user-safe appointment-registration failure and its category.
 */
public final class AppointmentRegistrationException extends Exception {

    /**
     * Categories that the future controller can map to suitable UI feedback.
     */
    public enum Reason {
        VALIDATION,
        DUPLICATE_APPOINTMENT_NUMBER,
        DENTIST_SCHEDULE_CONFLICT,
        PATIENT_SCHEDULE_CONFLICT,
        INACTIVE_DENTIST,
        INACTIVE_TREATMENT,
        INACTIVE_USER,
        DATABASE_FAILURE
    }

    private final Reason reason;

    public AppointmentRegistrationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AppointmentRegistrationException(
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
