package com.mycompany.sunrisedentalclinicmanagementsystem.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Read-only billing calculation built from current database records.
 *
 * <p>The future billing interface can display this information without
 * accepting treatment prices or totals from user input.</p>
 */
public record BillingCalculation(
        long appointmentId,
        String appointmentNumber,
        String appointmentStatus,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        String patientNumber,
        String patientName,
        String contactNumber,
        String dentistName,
        String treatmentCode,
        String treatmentName,
        BigDecimal treatmentPrice,
        BigDecimal consultationFee,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount
) {
}
