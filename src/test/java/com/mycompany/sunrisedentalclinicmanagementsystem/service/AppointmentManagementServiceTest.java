package com.mycompany.sunrisedentalclinicmanagementsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppointmentManagementServiceTest {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Colombo");
    private static final LocalDate CURRENT_DATE = LocalDate.of(2026, 8, 29);
    private static final LocalTime CURRENT_TIME = LocalTime.of(10, 0);

    private AppointmentManagementService service;

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
        service = new AppointmentManagementService(fixedClock);
    }

    @Test
    void normalizesValidAppointmentNumber() throws Exception {
        assertEquals(
                "APT-1001",
                service.normalizeAppointmentNumber("  apt-1001  ")
        );
    }

    @Test
    void rejectsEmptyAppointmentNumber() {
        AppointmentManagementException exception = assertThrows(
                AppointmentManagementException.class,
                () -> service.normalizeAppointmentNumber("   ")
        );

        assertEquals(
                AppointmentManagementException.Reason.VALIDATION,
                exception.getReason()
        );
    }

    @Test
    void rejectsInvalidAppointmentNumber() {
        assertThrows(
                AppointmentManagementException.class,
                () -> service.normalizeAppointmentNumber("APT 1001!")
        );
    }

    @Test
    void normalizesValidUpdateData() throws Exception {
        AppointmentUpdateRequest normalized = service.normalizeAndValidateUpdate(
                new AppointmentUpdateRequest(
                        10,
                        20,
                        "  Anjali   Marie ",
                        " Perera ",
                        " 12   Main Road ",
                        "  ",
                        " Colombo ",
                        "071 234-5678",
                        2,
                        3,
                        CURRENT_DATE.plusDays(1),
                        LocalTime.of(9, 30),
                        "  Follow-up visit  "
                )
        );

        assertEquals("Anjali Marie", normalized.patientFirstName());
        assertEquals("Perera", normalized.patientLastName());
        assertEquals("12 Main Road", normalized.addressLine1());
        assertNull(normalized.addressLine2());
        assertEquals("Colombo", normalized.city());
        assertEquals("0712345678", normalized.contactNumber());
        assertEquals("Follow-up visit", normalized.notes());
    }

    @Test
    void rejectsMissingRequiredPatientFields() {
        AppointmentUpdateRequest missingFirstName = validRequest(
                CURRENT_DATE.plusDays(1),
                LocalTime.of(9, 0),
                "0712345678",
                1,
                2
        );
        missingFirstName = new AppointmentUpdateRequest(
                missingFirstName.appointmentId(),
                missingFirstName.patientId(),
                " ",
                missingFirstName.patientLastName(),
                missingFirstName.addressLine1(),
                missingFirstName.addressLine2(),
                missingFirstName.city(),
                missingFirstName.contactNumber(),
                missingFirstName.dentistId(),
                missingFirstName.treatmentId(),
                missingFirstName.appointmentDate(),
                missingFirstName.appointmentTime(),
                missingFirstName.notes()
        );

        AppointmentUpdateRequest missingAddress = new AppointmentUpdateRequest(
                10,
                20,
                "Anjali",
                "Perera",
                null,
                null,
                "Colombo",
                "0712345678",
                1,
                2,
                CURRENT_DATE.plusDays(1),
                LocalTime.of(9, 0),
                null
        );

        AppointmentUpdateRequest finalMissingFirstName = missingFirstName;
        assertThrows(
                AppointmentManagementException.class,
                () -> service.normalizeAndValidateUpdate(finalMissingFirstName)
        );
        assertThrows(
                AppointmentManagementException.class,
                () -> service.normalizeAndValidateUpdate(missingAddress)
        );
    }

    @Test
    void rejectsInvalidContactNumber() {
        AppointmentUpdateRequest request = validRequest(
                CURRENT_DATE.plusDays(1),
                LocalTime.of(9, 0),
                "12345",
                1,
                2
        );

        assertThrows(
                AppointmentManagementException.class,
                () -> service.normalizeAndValidateUpdate(request)
        );
    }

    @Test
    void rejectsPastAppointmentDate() {
        AppointmentUpdateRequest request = validRequest(
                CURRENT_DATE.minusDays(1),
                LocalTime.of(11, 0),
                "0712345678",
                1,
                2
        );

        assertThrows(
                AppointmentManagementException.class,
                () -> service.normalizeAndValidateUpdate(request)
        );
    }

    @Test
    void rejectsNonFutureTimeForCurrentDate() {
        AppointmentUpdateRequest request = validRequest(
                CURRENT_DATE,
                CURRENT_TIME,
                "0712345678",
                1,
                2
        );

        assertThrows(
                AppointmentManagementException.class,
                () -> service.normalizeAndValidateUpdate(request)
        );
    }

    @Test
    void rejectsMissingDentistAndTreatmentSelections() {
        AppointmentUpdateRequest missingDentist = validRequest(
                CURRENT_DATE.plusDays(1),
                LocalTime.of(9, 0),
                "0712345678",
                0,
                2
        );
        AppointmentUpdateRequest missingTreatment = validRequest(
                CURRENT_DATE.plusDays(1),
                LocalTime.of(9, 0),
                "0712345678",
                1,
                0
        );

        assertThrows(
                AppointmentManagementException.class,
                () -> service.normalizeAndValidateUpdate(missingDentist)
        );
        assertThrows(
                AppointmentManagementException.class,
                () -> service.normalizeAndValidateUpdate(missingTreatment)
        );
    }

    private AppointmentUpdateRequest validRequest(
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            String contactNumber,
            long dentistId,
            long treatmentId
    ) {
        return new AppointmentUpdateRequest(
                10,
                20,
                "Anjali",
                "Perera",
                "12 Main Road",
                null,
                "Colombo",
                contactNumber,
                dentistId,
                treatmentId,
                appointmentDate,
                appointmentTime,
                null
        );
    }
}
