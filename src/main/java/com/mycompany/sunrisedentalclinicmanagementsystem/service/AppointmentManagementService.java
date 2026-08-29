package com.mycompany.sunrisedentalclinicmanagementsystem.service;

import com.mycompany.sunrisedentalclinicmanagementsystem.dao.AppointmentDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.dao.DentistDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.dao.PatientDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.dao.TreatmentDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.database.DatabaseConnection;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Appointment;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.AppointmentDetails;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Patient;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Coordinates searching, transactional updates, and soft cancellation of
 * existing appointments.
 */
public final class AppointmentManagementService {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Colombo");
    private static final Pattern APPOINTMENT_NUMBER_PATTERN
            = Pattern.compile("[A-Z0-9]+(?:-[A-Z0-9]+)*");
    private static final Pattern LOCAL_PHONE_PATTERN
            = Pattern.compile("0\\d{9}");
    private static final Pattern INTERNATIONAL_PHONE_PATTERN
            = Pattern.compile("\\+94\\d{9}");

    private static final int MIN_APPOINTMENT_NUMBER_LENGTH = 3;
    private static final int MAX_APPOINTMENT_NUMBER_LENGTH = 25;
    private static final int MAX_PATIENT_NAME_LENGTH = 60;
    private static final int MAX_ADDRESS_LENGTH = 150;
    private static final int MAX_CITY_LENGTH = 80;
    private static final int MAX_NOTES_LENGTH = 10_000;

    private final AppointmentDAO appointmentDAO;
    private final PatientDAO patientDAO;
    private final DentistDAO dentistDAO;
    private final TreatmentDAO treatmentDAO;
    private final Clock clock;

    public AppointmentManagementService() {
        this(
                new AppointmentDAO(),
                new PatientDAO(),
                new DentistDAO(),
                new TreatmentDAO(),
                Clock.system(CLINIC_ZONE)
        );
    }

    AppointmentManagementService(Clock clock) {
        this(
                new AppointmentDAO(),
                new PatientDAO(),
                new DentistDAO(),
                new TreatmentDAO(),
                clock
        );
    }

    AppointmentManagementService(
            AppointmentDAO appointmentDAO,
            PatientDAO patientDAO,
            DentistDAO dentistDAO,
            TreatmentDAO treatmentDAO,
            Clock clock
    ) {
        this.appointmentDAO = appointmentDAO;
        this.patientDAO = patientDAO;
        this.dentistDAO = dentistDAO;
        this.treatmentDAO = treatmentDAO;
        this.clock = clock;
    }

    /**
     * Searches for one appointment using its normalized public number.
     */
    public Optional<AppointmentDetails> searchByAppointmentNumber(
            String appointmentNumber
    ) throws AppointmentManagementException {
        String normalizedNumber = normalizeAppointmentNumber(appointmentNumber);

        try {
            return appointmentDAO.findDetailsByAppointmentNumber(normalizedNumber);
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    /**
     * Updates linked patient and appointment fields atomically.
     */
    public void update(AppointmentUpdateRequest request)
            throws AppointmentManagementException {
        AppointmentUpdateRequest normalizedRequest
                = normalizeAndValidateUpdate(request);

        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);
                updateWithinTransaction(connection, normalizedRequest);
                connection.commit();
            } catch (AppointmentManagementException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw translateDatabaseFailure(exception);
            } finally {
                restoreAutoCommit(connection, originalAutoCommit);
            }
        } catch (AppointmentManagementException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    /**
     * Soft-cancels an appointment while preserving all related history.
     */
    public void cancel(long appointmentId)
            throws AppointmentManagementException {
        if (appointmentId <= 0) {
            throw notFound();
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);
                AppointmentDetails details = appointmentDAO
                        .findDetailsByIdForUpdate(connection, appointmentId)
                        .orElseThrow(this::notFound);

                if ("CANCELLED".equals(details.status())) {
                    throw alreadyCancelled();
                }

                if (!appointmentDAO.cancelAppointment(connection, appointmentId)) {
                    throw notFound();
                }

                connection.commit();
            } catch (AppointmentManagementException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw databaseFailure(exception);
            } finally {
                restoreAutoCommit(connection, originalAutoCommit);
            }
        } catch (AppointmentManagementException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    String normalizeAppointmentNumber(String appointmentNumber)
            throws AppointmentManagementException {
        String normalized = normalizeRequiredSingleLine(
                appointmentNumber,
                "Appointment number"
        ).toUpperCase(Locale.ROOT);

        if (normalized.length() < MIN_APPOINTMENT_NUMBER_LENGTH
                || normalized.length() > MAX_APPOINTMENT_NUMBER_LENGTH) {
            throw validationError(
                    "Appointment number must contain between 3 and 25 characters."
            );
        }
        if (!APPOINTMENT_NUMBER_PATTERN.matcher(normalized).matches()) {
            throw validationError(
                    "Appointment number may contain only letters, numbers, "
                            + "and single hyphens between groups."
            );
        }
        return normalized;
    }

    AppointmentUpdateRequest normalizeAndValidateUpdate(
            AppointmentUpdateRequest request
    ) throws AppointmentManagementException {
        if (request == null) {
            throw validationError("Appointment update data is required.");
        }
        if (request.appointmentId() <= 0 || request.patientId() <= 0) {
            throw validationError("A valid loaded appointment is required.");
        }

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

        if (request.dentistId() <= 0) {
            throw validationError("Please select a dentist.");
        }
        if (request.treatmentId() <= 0) {
            throw validationError("Please select a treatment.");
        }

        validateDateAndTime(request.appointmentDate(), request.appointmentTime());

        String notes = normalizeOptionalMultiline(request.notes());
        ensureMaximumLength(notes, MAX_NOTES_LENGTH, "Notes");

        return new AppointmentUpdateRequest(
                request.appointmentId(),
                request.patientId(),
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
                notes
        );
    }

    private void updateWithinTransaction(
            Connection connection,
            AppointmentUpdateRequest request
    ) throws SQLException, AppointmentManagementException {
        AppointmentDetails existing = appointmentDAO.findDetailsByIdForUpdate(
                connection,
                request.appointmentId()
        ).orElseThrow(this::notFound);

        if (existing.patientId() != request.patientId()) {
            throw validationError(
                    "The loaded patient does not match this appointment. "
                            + "Please search again."
            );
        }
        if ("CANCELLED".equals(existing.status())) {
            throw new AppointmentManagementException(
                    AppointmentManagementException.Reason.ALREADY_CANCELLED,
                    "A cancelled appointment cannot be edited."
            );
        }
        if (!dentistDAO.isActive(connection, request.dentistId())) {
            throw new AppointmentManagementException(
                    AppointmentManagementException.Reason.INACTIVE_DENTIST,
                    "The selected dentist is unavailable or inactive."
            );
        }
        if (!treatmentDAO.isActive(connection, request.treatmentId())) {
            throw new AppointmentManagementException(
                    AppointmentManagementException.Reason.INACTIVE_TREATMENT,
                    "The selected treatment is unavailable or inactive."
            );
        }
        if (appointmentDAO.hasDentistScheduleConflict(
                connection,
                request.dentistId(),
                request.appointmentDate(),
                request.appointmentTime(),
                request.appointmentId()
        )) {
            throw dentistScheduleConflict();
        }
        if (appointmentDAO.hasPatientScheduleConflict(
                connection,
                request.patientId(),
                request.appointmentDate(),
                request.appointmentTime(),
                request.appointmentId()
        )) {
            throw patientScheduleConflict();
        }

        Patient patient = new Patient();
        patient.setPatientId(request.patientId());
        patient.setFirstName(request.patientFirstName());
        patient.setLastName(request.patientLastName());
        patient.setAddressLine1(request.addressLine1());
        patient.setAddressLine2(request.addressLine2());
        patient.setCity(request.city());
        patient.setPhone(request.contactNumber());
        patientDAO.update(connection, patient);

        Appointment appointment = new Appointment();
        appointment.setAppointmentId(request.appointmentId());
        appointment.setDentistId(request.dentistId());
        appointment.setTreatmentId(request.treatmentId());
        appointment.setAppointmentDate(request.appointmentDate());
        appointment.setAppointmentTime(request.appointmentTime());
        appointment.setNotes(request.notes());
        appointmentDAO.updateAppointment(connection, appointment);
    }

    private void validateDateAndTime(LocalDate date, LocalTime time)
            throws AppointmentManagementException {
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
            throws AppointmentManagementException {
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
            throws AppointmentManagementException {
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
    ) throws AppointmentManagementException {
        if (value != null && value.length() > maximumLength) {
            throw validationError(
                    fieldName + " must not exceed " + maximumLength + " characters."
            );
        }
    }

    private AppointmentManagementException translateDatabaseFailure(
            SQLException exception
    ) {
        String message = exception.getMessage() == null
                ? ""
                : exception.getMessage().toLowerCase(Locale.ROOT);

        if (message.contains("uq_appointments_dentist_schedule")) {
            return dentistScheduleConflict(exception);
        }
        if (message.contains("uq_appointments_patient_schedule")) {
            return patientScheduleConflict(exception);
        }
        return databaseFailure(exception);
    }

    private AppointmentManagementException validationError(String message) {
        return new AppointmentManagementException(
                AppointmentManagementException.Reason.VALIDATION,
                message
        );
    }

    private AppointmentManagementException notFound() {
        return new AppointmentManagementException(
                AppointmentManagementException.Reason.NOT_FOUND,
                "The requested appointment could not be found."
        );
    }

    private AppointmentManagementException alreadyCancelled() {
        return new AppointmentManagementException(
                AppointmentManagementException.Reason.ALREADY_CANCELLED,
                "This appointment has already been cancelled."
        );
    }

    private AppointmentManagementException dentistScheduleConflict() {
        return dentistScheduleConflict(null);
    }

    private AppointmentManagementException dentistScheduleConflict(
            Throwable cause
    ) {
        return new AppointmentManagementException(
                AppointmentManagementException.Reason.DENTIST_SCHEDULE_CONFLICT,
                "The selected dentist already has an appointment at that date and time.",
                cause
        );
    }

    private AppointmentManagementException patientScheduleConflict() {
        return patientScheduleConflict(null);
    }

    private AppointmentManagementException patientScheduleConflict(
            Throwable cause
    ) {
        return new AppointmentManagementException(
                AppointmentManagementException.Reason.PATIENT_SCHEDULE_CONFLICT,
                "The patient already has an appointment at that date and time.",
                cause
        );
    }

    private AppointmentManagementException databaseFailure(Throwable cause) {
        return new AppointmentManagementException(
                AppointmentManagementException.Reason.DATABASE_FAILURE,
                "The appointment operation could not be completed because the "
                        + "database is currently unavailable. Please try again.",
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
