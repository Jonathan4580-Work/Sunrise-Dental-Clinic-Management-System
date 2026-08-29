package com.mycompany.sunrisedentalclinicmanagementsystem.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Read-only joined view of an appointment and its related clinic records.
 *
 * <p>Database identifiers support internal coordination and must not be
 * presented as editable values in the user interface.</p>
 */
public record AppointmentDetails(
        long appointmentId,
        long patientId,
        long dentistId,
        long treatmentId,
        String appointmentNumber,
        String patientNumber,
        String firstName,
        String lastName,
        String addressLine1,
        String addressLine2,
        String city,
        String contactNumber,
        String dentistRegistrationNumber,
        String dentistName,
        String dentistSpecialization,
        String treatmentCode,
        String treatmentName,
        String treatmentDescription,
        BigDecimal treatmentPrice,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        String status,
        String notes,
        String createdByUsername,
        String createdByFullName,
        LocalDateTime updatedAt
) {
}
