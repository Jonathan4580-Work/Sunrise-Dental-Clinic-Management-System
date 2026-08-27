package com.mycompany.sunrisedentalclinicmanagementsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppointmentRegistrationServiceTest {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Colombo");
    private static final LocalDate CURRENT_DATE = LocalDate.of(2026, 8, 27);
    private static final LocalTime CURRENT_TIME = LocalTime.of(10, 0);

    private AppointmentRegistrationService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                ZonedDateTime.of(
                        CURRENT_DATE,
                        CURRENT_TIME,
                        CLINIC_ZONE
                ).toInstant(),
                CLINIC_ZONE
        );
        service = new AppointmentRegistrationService(fixedClock);
    }

    @Test
    void normalizesValidRegistrationData() throws Exception {
        AppointmentRegistrationRequest normalized = service.normalizeAndValidate(
                new AppointmentRegistrationRequest(
                        "  apt-1001  ",
                        "  Anjali   Marie ",
                        " Perera ",
                        " 12   Main Road ",
                        "   ",
                        " Colombo ",
                        "071 234-5678",
                        1,
                        2,
                        CURRENT_DATE.plusDays(1),
                        LocalTime.of(9, 30),
                        "  First visit  ",
                        " admin "
                )
        );

        assertEquals("APT-1001", normalized.appointmentNumber());
        assertEquals("Anjali Marie", normalized.patientFirstName());
        assertEquals("Perera", normalized.patientLastName());
        assertEquals("12 Main Road", normalized.addressLine1());
        assertNull(normalized.addressLine2());
        assertEquals("Colombo", normalized.city());
        assertEquals("0712345678", normalized.contactNumber());
        assertEquals("First visit", normalized.notes());
        assertEquals("admin", normalized.authenticatedUsername());
    }

    @Test
    void normalizesInternationalSriLankanContactNumber() throws Exception {
        AppointmentRegistrationRequest request = validRequest(
                CURRENT_DATE.plusDays(1),
                LocalTime.of(9, 0),
                "0094 (71) 234-5678"
        );

        AppointmentRegistrationRequest normalized
                = service.normalizeAndValidate(request);

        assertEquals("+94712345678", normalized.contactNumber());
    }

    @Test
    void rejectsInvalidAppointmentNumber() {
        AppointmentRegistrationRequest request = new AppointmentRegistrationRequest(
                "APT 1001!",
                "Anjali",
                "Perera",
                "12 Main Road",
                null,
                "Colombo",
                "0712345678",
                1,
                2,
                CURRENT_DATE.plusDays(1),
                LocalTime.of(9, 0),
                null,
                "admin"
        );

        AppointmentRegistrationException exception = assertThrows(
                AppointmentRegistrationException.class,
                () -> service.normalizeAndValidate(request)
        );

        assertEquals(
                AppointmentRegistrationException.Reason.VALIDATION,
                exception.getReason()
        );
    }

    @Test
    void rejectsPastDateAndNonFutureTimeToday() {
        AppointmentRegistrationException pastDateException = assertThrows(
                AppointmentRegistrationException.class,
                () -> service.normalizeAndValidate(validRequest(
                        CURRENT_DATE.minusDays(1),
                        LocalTime.of(11, 0),
                        "0712345678"
                ))
        );
        AppointmentRegistrationException pastTimeException = assertThrows(
                AppointmentRegistrationException.class,
                () -> service.normalizeAndValidate(validRequest(
                        CURRENT_DATE,
                        CURRENT_TIME,
                        "0712345678"
                ))
        );

        assertEquals(
                AppointmentRegistrationException.Reason.VALIDATION,
                pastDateException.getReason()
        );
        assertEquals(
                AppointmentRegistrationException.Reason.VALIDATION,
                pastTimeException.getReason()
        );
        assertNotEquals(pastDateException.getMessage(), pastTimeException.getMessage());
    }

    @Test
    void rejectsMissingSelectionsAndInvalidContactNumber() {
        AppointmentRegistrationRequest missingDentist
                = new AppointmentRegistrationRequest(
                        "APT-1001",
                        "Anjali",
                        "Perera",
                        "12 Main Road",
                        null,
                        null,
                        "0712345678",
                        0,
                        2,
                        CURRENT_DATE.plusDays(1),
                        LocalTime.of(9, 0),
                        null,
                        "admin"
                );
        AppointmentRegistrationRequest invalidPhone = validRequest(
                CURRENT_DATE.plusDays(1),
                LocalTime.of(9, 0),
                "12345"
        );

        assertThrows(
                AppointmentRegistrationException.class,
                () -> service.normalizeAndValidate(missingDentist)
        );
        assertThrows(
                AppointmentRegistrationException.class,
                () -> service.normalizeAndValidate(invalidPhone)
        );
    }

    @Test
    void generatesTwentyCharacterHumanReadablePatientNumbers() {
        Set<String> generatedNumbers = new HashSet<>();

        for (int index = 0; index < 200; index++) {
            String patientNumber = service.generatePatientNumber();
            assertTrue(patientNumber.matches("PAT-20260827-[23456789A-HJ-NP-Z]{7}"));
            assertEquals(20, patientNumber.length());
            assertTrue(generatedNumbers.add(patientNumber));
        }
    }

    private AppointmentRegistrationRequest validRequest(
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            String contactNumber
    ) {
        return new AppointmentRegistrationRequest(
                "APT-1001",
                "Anjali",
                "Perera",
                "12 Main Road",
                null,
                "Colombo",
                contactNumber,
                1,
                2,
                appointmentDate,
                appointmentTime,
                null,
                "admin"
        );
    }
}
