package com.mycompany.sunrisedentalclinicmanagementsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BillingServiceTest {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Colombo");
    private static final LocalDate CURRENT_DATE = LocalDate.of(2026, 8, 30);

    private BillingService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                ZonedDateTime.of(
                        CURRENT_DATE,
                        LocalTime.of(10, 0),
                        CLINIC_ZONE
                ).toInstant(),
                CLINIC_ZONE
        );
        service = new BillingService(fixedClock);
    }

    @Test
    void normalizesAppointmentNumberAndStaffUsername() throws Exception {
        assertEquals(
                "APT-1001",
                service.normalizeAppointmentNumber("  apt-1001  ")
        );
        assertEquals("admin user", service.normalizeUsername(" admin   user "));
    }

    @Test
    void rejectsMissingOrInvalidAppointmentNumber() {
        BillingException missing = assertThrows(
                BillingException.class,
                () -> service.normalizeAppointmentNumber("   ")
        );
        BillingException invalid = assertThrows(
                BillingException.class,
                () -> service.normalizeAppointmentNumber("APT 1001!")
        );

        assertEquals(BillingException.Reason.VALIDATION, missing.getReason());
        assertEquals(BillingException.Reason.VALIDATION, invalid.getReason());
    }

    @Test
    void calculatesTreatmentPricePlusConsultationFee() throws Exception {
        assertEquals(
                new BigDecimal("11000.00"),
                service.calculateSubtotal(
                        new BigDecimal("8500.00"),
                        new BigDecimal("2500.00")
                )
        );
    }

    @Test
    void avoidsDoubleChargingConsultationTreatment() throws Exception {
        BigDecimal consultationFee = service.determineConsultationFee(
                "TRT-001",
                new BigDecimal("2500.00")
        );

        assertEquals(new BigDecimal("0.00"), consultationFee);
        assertEquals(
                new BigDecimal("2500.00"),
                service.calculateSubtotal(
                        new BigDecimal("2500.00"),
                        consultationFee
                )
        );
    }

    @Test
    void rejectsNegativeOrMissingMonetaryValues() {
        BillingException negative = assertThrows(
                BillingException.class,
                () -> service.calculateSubtotal(
                        new BigDecimal("-1.00"),
                        new BigDecimal("2500.00")
                )
        );
        BillingException missing = assertThrows(
                BillingException.class,
                () -> service.calculateSubtotal(null, new BigDecimal("2500.00"))
        );

        assertEquals(BillingException.Reason.INVALID_AMOUNT, negative.getReason());
        assertEquals(BillingException.Reason.INVALID_AMOUNT, missing.getReason());
    }

    @Test
    void rejectsUnsupportedMoneyScaleAndDatabaseOverflow() {
        assertThrows(
                BillingException.class,
                () -> service.calculateSubtotal(
                        new BigDecimal("10.001"),
                        BigDecimal.ZERO
                )
        );
        assertThrows(
                BillingException.class,
                () -> service.calculateSubtotal(
                        new BigDecimal("99999999.99"),
                        new BigDecimal("0.01")
                )
        );
    }

    @Test
    void generatesUniqueHumanReadableBillNumbers() {
        Set<String> generatedNumbers = new HashSet<>();

        for (int index = 0; index < 200; index++) {
            String billNumber = service.generateBillNumber();
            assertTrue(billNumber.matches(
                    "BIL-20260830-[23456789A-HJ-NP-Z]{7}"
            ));
            assertEquals(20, billNumber.length());
            assertTrue(generatedNumbers.add(billNumber));
        }
    }
}
