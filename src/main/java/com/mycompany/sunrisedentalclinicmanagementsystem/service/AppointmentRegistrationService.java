package com.mycompany.sunrisedentalclinicmanagementsystem.service;

import com.mycompany.sunrisedentalclinicmanagementsystem.dao.AppointmentDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.dao.DentistDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.dao.PatientDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.dao.TreatmentDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.dao.UserDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.database.DatabaseConnection;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Appointment;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Patient;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Pattern;

/**
 * Coordinates validation and transactional registration of a new patient and
 * appointment.
 */
public final class AppointmentRegistrationService {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Colombo");
    private static final DateTimeFormatter PATIENT_NUMBER_DATE_FORMAT
            = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Pattern APPOINTMENT_NUMBER_PATTERN
            = Pattern.compile("[A-Z0-9]+(?:-[A-Z0-9]+)*");
    private static final Pattern LOCAL_PHONE_PATTERN
            = Pattern.compile("0\\d{9}");
    private static final Pattern INTERNATIONAL_PHONE_PATTERN
            = Pattern.compile("\\+94\\d{9}");
    private static final char[] PATIENT_NUMBER_ALPHABET
            = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private static final int MIN_APPOINTMENT_NUMBER_LENGTH = 3;
    private static final int MAX_APPOINTMENT_NUMBER_LENGTH = 25;
    private static final int MAX_PATIENT_NAME_LENGTH = 60;
    private static final int MAX_ADDRESS_LENGTH = 150;
    private static final int MAX_CITY_LENGTH = 80;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MAX_NOTES_LENGTH = 10_000;

    private final PatientDAO patientDAO;
    private final AppointmentDAO appointmentDAO;
    private final DentistDAO dentistDAO;
    private final TreatmentDAO treatmentDAO;
    private final UserDAO userDAO;
    private final Clock clock;
    private final SecureRandom secureRandom;

    public AppointmentRegistrationService() {
        this(
                new PatientDAO(),
                new AppointmentDAO(),
                new DentistDAO(),
                new TreatmentDAO(),
                new UserDAO(),
                Clock.system(CLINIC_ZONE),
                new SecureRandom()
        );
    }

    AppointmentRegistrationService(Clock clock) {
        this(
                new PatientDAO(),
                new AppointmentDAO(),
                new DentistDAO(),
                new TreatmentDAO(),
                new UserDAO(),
                clock,
                new SecureRandom()
        );
    }

    AppointmentRegistrationService(
            PatientDAO patientDAO,
            AppointmentDAO appointmentDAO,
            DentistDAO dentistDAO,
            TreatmentDAO treatmentDAO,
            UserDAO userDAO,
            Clock clock,
            SecureRandom secureRandom
    ) {
        this.patientDAO = patientDAO;
        this.appointmentDAO = appointmentDAO;
        this.dentistDAO = dentistDAO;
        this.treatmentDAO = treatmentDAO;
        this.userDAO = userDAO;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    /**
     * Registers a new patient and linked appointment atomically.
     *
     * @param request registration data from the future controller
     * @return generated appointment ID
     * @throws AppointmentRegistrationException if validation, a business rule,
     *         or database access prevents registration
     */
    public long register(AppointmentRegistrationRequest request)
            throws AppointmentRegistrationException {
        AppointmentRegistrationRequest normalizedRequest
                = normalizeAndValidate(request);

        verifyActiveSelections(normalizedRequest);

        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);
                long appointmentId = registerWithinTransaction(
                        connection,
                        normalizedRequest
                );
                connection.commit();
                return appointmentId;
            } catch (AppointmentRegistrationException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw translateDatabaseFailure(exception);
            } finally {
                restoreAutoCommit(connection, originalAutoCommit);
            }
        } catch (AppointmentRegistrationException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    AppointmentRegistrationRequest normalizeAndValidate(
            AppointmentRegistrationRequest request
    ) throws AppointmentRegistrationException {
        if (request == null) {
            throw validationError("Appointment registration data is required.");
        }

        String appointmentNumber = normalizeRequiredSingleLine(
                request.appointmentNumber(),
                "Appointment number"
        ).toUpperCase(Locale.ROOT);
        validateAppointmentNumber(appointmentNumber);

        String firstName = normalizeRequiredSingleLine(
                request.patientFirstName(),
                "Patient first name"
        );
        ensureMaximumLength(
                firstName,
                MAX_PATIENT_NAME_LENGTH,
                "Patient first name"
        );

        String lastName = normalizeRequiredSingleLine(
                request.patientLastName(),
                "Patient last name"
        );
        ensureMaximumLength(
                lastName,
                MAX_PATIENT_NAME_LENGTH,
                "Patient last name"
        );

        String addressLine1 = normalizeRequiredSingleLine(
                request.addressLine1(),
                "Address Line 1"
        );
        ensureMaximumLength(addressLine1, MAX_ADDRESS_LENGTH, "Address Line 1");

        String addressLine2 = normalizeOptionalSingleLine(request.addressLine2());
        ensureMaximumLength(addressLine2, MAX_ADDRESS_LENGTH, "Address Line 2");

        String city = normalizeOptionalSingleLine(request.city());
        ensureMaximumLength(city, MAX_CITY_LENGTH, "City");

        String contactNumber = normalizeContactNumber(request.contactNumber());
        validateDateAndTime(request.appointmentDate(), request.appointmentTime());

        if (request.dentistId() <= 0) {
            throw validationError("Please select a dentist.");
        }
        if (request.treatmentId() <= 0) {
            throw validationError("Please select a treatment.");
        }

        String notes = normalizeOptionalMultiline(request.notes());
        ensureMaximumLength(notes, MAX_NOTES_LENGTH, "Notes");

        String username = normalizeRequiredSingleLine(
                request.authenticatedUsername(),
                "Authenticated staff username"
        );
        ensureMaximumLength(username, MAX_USERNAME_LENGTH, "Staff username");

        return new AppointmentRegistrationRequest(
                appointmentNumber,
                firstName,
                lastName,
                addressLine1,
                addressLine2,
                city,
                contactNumber,
                request.dentistId(),
                request.treatmentId(),
                request.appointmentDate(),
                request.appointmentTime(),
                notes,
                username
        );
    }

    String generatePatientNumber() {
        StringBuilder randomPart = new StringBuilder(7);
        for (int index = 0; index < 7; index++) {
            int characterIndex = secureRandom.nextInt(
                    PATIENT_NUMBER_ALPHABET.length
            );
            randomPart.append(PATIENT_NUMBER_ALPHABET[characterIndex]);
        }

        String datePart = LocalDate.now(clock).format(
                PATIENT_NUMBER_DATE_FORMAT
        );
        return "PAT-" + datePart + "-" + randomPart;
    }

    private long registerWithinTransaction(
            Connection connection,
            AppointmentRegistrationRequest request
    ) throws SQLException, AppointmentRegistrationException {
        OptionalLong activeUserId = userDAO.findActiveUserIdByUsername(
                connection,
                request.authenticatedUsername()
        );
        if (activeUserId.isEmpty()) {
            throw new AppointmentRegistrationException(
                    AppointmentRegistrationException.Reason.INACTIVE_USER,
                    "The authenticated staff account is not active or no longer exists."
            );
        }

        if (appointmentDAO.appointmentNumberExists(
                connection,
                request.appointmentNumber()
        )) {
            throw duplicateAppointmentNumber();
        }

        if (appointmentDAO.hasDentistScheduleConflict(
                connection,
                request.dentistId(),
                request.appointmentDate(),
                request.appointmentTime()
        )) {
            throw dentistScheduleConflict();
        }

        Patient patient = createPatient(request);
        long patientId = patientDAO.insert(connection, patient);

        if (appointmentDAO.hasPatientScheduleConflict(
                connection,
                patientId,
                request.appointmentDate(),
                request.appointmentTime()
        )) {
            throw patientScheduleConflict();
        }

        Appointment appointment = new Appointment(
                request.appointmentNumber(),
                patientId,
                request.dentistId(),
                request.treatmentId(),
                request.appointmentDate(),
                request.appointmentTime(),
                "SCHEDULED",
                request.notes(),
                activeUserId.getAsLong()
        );

        return appointmentDAO.insert(connection, appointment);
    }

    private Patient createPatient(AppointmentRegistrationRequest request) {
        return new Patient(
                generatePatientNumber(),
                request.patientFirstName(),
                request.patientLastName(),
                request.contactNumber(),
                request.addressLine1(),
                request.addressLine2(),
                request.city()
        );
    }

    private void verifyActiveSelections(AppointmentRegistrationRequest request)
            throws AppointmentRegistrationException {
        try {
            boolean activeDentist = dentistDAO.findAllActive().stream()
                    .anyMatch(dentist -> dentist.getDentistId()
                    == request.dentistId());
            if (!activeDentist) {
                throw new AppointmentRegistrationException(
                        AppointmentRegistrationException.Reason.INACTIVE_DENTIST,
                        "The selected dentist is unavailable or inactive."
                );
            }

            boolean activeTreatment = treatmentDAO.findAllActive().stream()
                    .anyMatch(treatment -> treatment.getTreatmentId()
                    == request.treatmentId());
            if (!activeTreatment) {
                throw new AppointmentRegistrationException(
                        AppointmentRegistrationException.Reason.INACTIVE_TREATMENT,
                        "The selected treatment is unavailable or inactive."
                );
            }
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    private void validateAppointmentNumber(String appointmentNumber)
            throws AppointmentRegistrationException {
        if (appointmentNumber.length() < MIN_APPOINTMENT_NUMBER_LENGTH
                || appointmentNumber.length() > MAX_APPOINTMENT_NUMBER_LENGTH) {
            throw validationError(
                    "Appointment number must contain between 3 and 25 characters."
            );
        }
        if (!APPOINTMENT_NUMBER_PATTERN.matcher(appointmentNumber).matches()) {
            throw validationError(
                    "Appointment number may contain only letters, numbers, "
                            + "and single hyphens between groups."
            );
        }
    }

    private void validateDateAndTime(LocalDate date, LocalTime time)
            throws AppointmentRegistrationException {
        if (date == null) {
            throw validationError("Appointment date is required.");
        }
        if (time == null) {
            throw validationError("Appointment time is required.");
        }

        LocalDate currentDate = LocalDate.now(clock);
        if (date.isBefore(currentDate)) {
            throw validationError("Appointment date cannot be in the past.");
        }
        if (date.equals(currentDate) && !time.isAfter(LocalTime.now(clock))) {
            throw validationError(
                    "For today's appointments, the selected time must be in the future."
            );
        }
    }

    private String normalizeContactNumber(String value)
            throws AppointmentRegistrationException {
        String normalized = normalizeRequiredSingleLine(
                value,
                "Contact number"
        ).replaceAll("[\\s()\\-]", "");

        if (normalized.startsWith("0094")) {
            normalized = "+94" + normalized.substring(4);
        }

        if (!LOCAL_PHONE_PATTERN.matcher(normalized).matches()
                && !INTERNATIONAL_PHONE_PATTERN.matcher(normalized).matches()) {
            throw validationError(
                    "Enter a valid Sri Lankan contact number, such as "
                            + "0712345678 or +94712345678."
            );
        }

        return normalized;
    }

    private String normalizeRequiredSingleLine(String value, String fieldName)
            throws AppointmentRegistrationException {
        String normalized = normalizeOptionalSingleLine(value);
        if (normalized == null) {
            throw validationError(fieldName + " is required.");
        }
        return normalized;
    }

    private String normalizeOptionalSingleLine(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeOptionalMultiline(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    private void ensureMaximumLength(
            String value,
            int maximumLength,
            String fieldName
    ) throws AppointmentRegistrationException {
        if (value != null && value.length() > maximumLength) {
            throw validationError(
                    fieldName + " must not exceed " + maximumLength + " characters."
            );
        }
    }

    private AppointmentRegistrationException translateDatabaseFailure(
            SQLException exception
    ) {
        String databaseMessage = exception.getMessage() == null
                ? ""
                : exception.getMessage().toLowerCase(Locale.ROOT);

        if (databaseMessage.contains("uq_appointments_number")) {
            return duplicateAppointmentNumber(exception);
        }
        if (databaseMessage.contains("uq_appointments_dentist_schedule")) {
            return dentistScheduleConflict(exception);
        }
        if (databaseMessage.contains("uq_appointments_patient_schedule")) {
            return patientScheduleConflict(exception);
        }
        return databaseFailure(exception);
    }

    private AppointmentRegistrationException validationError(String message) {
        return new AppointmentRegistrationException(
                AppointmentRegistrationException.Reason.VALIDATION,
                message
        );
    }

    private AppointmentRegistrationException duplicateAppointmentNumber() {
        return duplicateAppointmentNumber(null);
    }

    private AppointmentRegistrationException duplicateAppointmentNumber(
            Throwable cause
    ) {
        return new AppointmentRegistrationException(
                AppointmentRegistrationException.Reason.DUPLICATE_APPOINTMENT_NUMBER,
                "That appointment number is already registered.",
                cause
        );
    }

    private AppointmentRegistrationException dentistScheduleConflict() {
        return dentistScheduleConflict(null);
    }

    private AppointmentRegistrationException dentistScheduleConflict(
            Throwable cause
    ) {
        return new AppointmentRegistrationException(
                AppointmentRegistrationException.Reason.DENTIST_SCHEDULE_CONFLICT,
                "The selected dentist already has an appointment at that date and time.",
                cause
        );
    }

    private AppointmentRegistrationException patientScheduleConflict() {
        return patientScheduleConflict(null);
    }

    private AppointmentRegistrationException patientScheduleConflict(
            Throwable cause
    ) {
        return new AppointmentRegistrationException(
                AppointmentRegistrationException.Reason.PATIENT_SCHEDULE_CONFLICT,
                "The patient already has an appointment at that date and time.",
                cause
        );
    }

    private AppointmentRegistrationException databaseFailure(Throwable cause) {
        return new AppointmentRegistrationException(
                AppointmentRegistrationException.Reason.DATABASE_FAILURE,
                "The appointment could not be registered because the database "
                        + "is currently unavailable. Please try again.",
                cause
        );
    }

    private void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    private void restoreAutoCommit(Connection connection, boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException ignored) {
            // The try-with-resources block still closes this connection.
        }
    }
}
