package com.mycompany.sunrisedentalclinicmanagementsystem.service;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Carries user-entered data required to register a patient and appointment.
 */
public record AppointmentRegistrationRequest(
        String appointmentNumber,
        String patientFirstName,
        String patientLastName,
        String addressLine1,
        String addressLine2,
        String city,
        String contactNumber,
        long dentistId,
        long treatmentId,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        String notes,
        String authenticatedUsername
) {
}
