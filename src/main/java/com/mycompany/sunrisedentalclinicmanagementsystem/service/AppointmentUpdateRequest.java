package com.mycompany.sunrisedentalclinicmanagementsystem.service;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Immutable input for updating an existing patient and appointment.
 *
 * <p>The identifiers originate from a loaded appointment and are never
 * entered or edited directly by the user.</p>
 */
public record AppointmentUpdateRequest(
        long appointmentId,
        long patientId,
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
        String notes
) {
}
