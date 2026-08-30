package com.mycompany.sunrisedentalclinicmanagementsystem.service;

import com.mycompany.sunrisedentalclinicmanagementsystem.dao.AppointmentDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.dao.BillDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.dao.TreatmentDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.dao.UserDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.database.DatabaseConnection;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.AppointmentDetails;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Bill;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.BillingCalculation;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.GeneratedBill;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Treatment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Pattern;

/**
 * Calculates treatment charges and creates one bill per appointment.
 */
public final class BillingService {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Colombo");
    private static final DateTimeFormatter BILL_NUMBER_DATE_FORMAT
            = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Pattern APPOINTMENT_NUMBER_PATTERN
            = Pattern.compile("[A-Z0-9]+(?:-[A-Z0-9]+)*");
    private static final char[] BILL_NUMBER_ALPHABET
            = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private static final String CONSULTATION_TREATMENT_CODE = "TRT-001";
    private static final String PAYMENT_STATUS_UNPAID = "UNPAID";
    private static final int MIN_APPOINTMENT_NUMBER_LENGTH = 3;
    private static final int MAX_APPOINTMENT_NUMBER_LENGTH = 25;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");
    private static final BigDecimal MAX_DATABASE_AMOUNT
            = new BigDecimal("99999999.99");

    private final AppointmentDAO appointmentDAO;
    private final BillDAO billDAO;
    private final TreatmentDAO treatmentDAO;
    private final UserDAO userDAO;
    private final Clock clock;
    private final SecureRandom secureRandom;

    public BillingService() {
        this(
                new AppointmentDAO(),
                new BillDAO(),
                new TreatmentDAO(),
                new UserDAO(),
                Clock.system(CLINIC_ZONE),
                new SecureRandom()
        );
    }

    BillingService(Clock clock) {
        this(
                new AppointmentDAO(),
                new BillDAO(),
                new TreatmentDAO(),
                new UserDAO(),
                clock,
                new SecureRandom()
        );
    }

    BillingService(
            AppointmentDAO appointmentDAO,
            BillDAO billDAO,
            TreatmentDAO treatmentDAO,
            UserDAO userDAO,
            Clock clock,
            SecureRandom secureRandom
    ) {
        this.appointmentDAO = appointmentDAO;
        this.billDAO = billDAO;
        this.treatmentDAO = treatmentDAO;
        this.userDAO = userDAO;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    /**
     * Calculates a bill preview entirely from current database values.
     *
     * @param appointmentNumber public appointment number entered by staff
     * @return treatment, consultation, and total cost details
     * @throws BillingException if the appointment cannot be billed
     */
    public BillingCalculation calculateBill(String appointmentNumber)
            throws BillingException {
        String normalizedNumber = normalizeAppointmentNumber(appointmentNumber);

        try (Connection connection = DatabaseConnection.getConnection()) {
            AppointmentDetails details = appointmentDAO
                    .findDetailsByAppointmentNumber(connection, normalizedNumber)
                    .orElseThrow(this::appointmentNotFound);
            return calculateForAppointment(connection, details, true);
        } catch (BillingException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    /**
     * Creates and stores an unpaid bill in one database transaction.
     *
     * @param appointmentNumber appointment being billed
     * @param authenticatedUsername staff member generating the bill
     * @return the newly stored bill and its authoritative calculation
     * @throws BillingException if validation or persistence fails
     */
    public GeneratedBill createBill(
            String appointmentNumber,
            String authenticatedUsername
    ) throws BillingException {
        String normalizedNumber = normalizeAppointmentNumber(appointmentNumber);
        String normalizedUsername = normalizeUsername(authenticatedUsername);

        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);
                GeneratedBill generatedBill = createWithinTransaction(
                        connection,
                        normalizedNumber,
                        normalizedUsername
                );
                connection.commit();
                return generatedBill;
            } catch (BillingException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw translateDatabaseFailure(exception);
            } finally {
                restoreAutoCommit(connection, originalAutoCommit);
            }
        } catch (BillingException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    String normalizeAppointmentNumber(String appointmentNumber)
            throws BillingException {
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

    String normalizeUsername(String username) throws BillingException {
        String normalized = normalizeRequiredSingleLine(
                username,
                "Authenticated staff username"
        );
        if (normalized.length() > MAX_USERNAME_LENGTH) {
            throw validationError(
                    "Staff username must not exceed 50 characters."
            );
        }
        return normalized;
    }

    BigDecimal determineConsultationFee(
            String appointmentTreatmentCode,
            BigDecimal configuredConsultationPrice
    ) throws BillingException {
        BigDecimal configuredFee = normalizeMoney(
                configuredConsultationPrice,
                "Consultation fee"
        );
        if (CONSULTATION_TREATMENT_CODE.equals(appointmentTreatmentCode)) {
            return ZERO_AMOUNT;
        }
        return configuredFee;
    }

    BigDecimal calculateSubtotal(
            BigDecimal treatmentPrice,
            BigDecimal consultationFee
    ) throws BillingException {
        BigDecimal normalizedTreatmentPrice = normalizeMoney(
                treatmentPrice,
                "Treatment price"
        );
        BigDecimal normalizedConsultationFee = normalizeMoney(
                consultationFee,
                "Consultation fee"
        );
        return normalizeMoney(
                normalizedTreatmentPrice.add(normalizedConsultationFee),
                "Bill subtotal"
        );
    }

    String generateBillNumber() {
        StringBuilder randomPart = new StringBuilder(7);
        for (int index = 0; index < 7; index++) {
            int characterIndex = secureRandom.nextInt(
                    BILL_NUMBER_ALPHABET.length
            );
            randomPart.append(BILL_NUMBER_ALPHABET[characterIndex]);
        }

        String datePart = LocalDate.now(clock).format(BILL_NUMBER_DATE_FORMAT);
        return "BIL-" + datePart + "-" + randomPart;
    }

    private GeneratedBill createWithinTransaction(
            Connection connection,
            String appointmentNumber,
            String authenticatedUsername
    ) throws SQLException, BillingException {
        AppointmentDetails details = appointmentDAO
                .findDetailsByAppointmentNumberForUpdate(
                        connection,
                        appointmentNumber
                )
                .orElseThrow(this::appointmentNotFound);

        BillingCalculation calculation = calculateForAppointment(
                connection,
                details,
                true
        );

        OptionalLong activeUserId = userDAO.findActiveUserIdByUsername(
                connection,
                authenticatedUsername
        );
        if (activeUserId.isEmpty()) {
            throw new BillingException(
                    BillingException.Reason.INACTIVE_USER,
                    "The authenticated staff account is not active or no longer exists."
            );
        }

        Bill bill = new Bill(
                generateBillNumber(),
                calculation.appointmentId(),
                calculation.subtotal(),
                calculation.discountAmount(),
                calculation.taxAmount(),
                calculation.totalAmount(),
                ZERO_AMOUNT,
                PAYMENT_STATUS_UNPAID,
                null,
                null,
                activeUserId.getAsLong()
        );
        billDAO.insert(connection, bill);
        bill.setCreatedAt(LocalDateTime.now(clock));
        return new GeneratedBill(bill, calculation);
    }

    private BillingCalculation calculateForAppointment(
            Connection connection,
            AppointmentDetails details,
            boolean rejectExistingBill
    ) throws SQLException, BillingException {
        validateAppointmentStatus(details.status());

        if (!treatmentDAO.isActive(connection, details.treatmentId())) {
            throw new BillingException(
                    BillingException.Reason.INACTIVE_TREATMENT,
                    "The appointment treatment is no longer active and cannot be billed."
            );
        }

        if (rejectExistingBill
                && billDAO.existsForAppointment(connection, details.appointmentId())) {
            throw duplicateBill();
        }

        Treatment consultationTreatment = treatmentDAO.findActiveByCode(
                connection,
                CONSULTATION_TREATMENT_CODE
        ).orElseThrow(() -> new BillingException(
                BillingException.Reason.CONSULTATION_FEE_UNAVAILABLE,
                "The clinic consultation fee is unavailable. Please ask an "
                        + "administrator to check treatment TRT-001."
        ));

        BigDecimal treatmentPrice = normalizeMoney(
                details.treatmentPrice(),
                "Treatment price"
        );
        BigDecimal consultationFee = determineConsultationFee(
                details.treatmentCode(),
                consultationTreatment.getPrice()
        );
        BigDecimal subtotal = calculateSubtotal(
                treatmentPrice,
                consultationFee
        );

        return new BillingCalculation(
                details.appointmentId(),
                details.appointmentNumber(),
                details.status(),
                details.appointmentDate(),
                details.appointmentTime(),
                details.patientNumber(),
                formatPatientName(details.firstName(), details.lastName()),
                details.contactNumber(),
                details.dentistName(),
                details.treatmentCode(),
                details.treatmentName(),
                treatmentPrice,
                consultationFee,
                subtotal,
                ZERO_AMOUNT,
                ZERO_AMOUNT,
                subtotal
        );
    }

    private void validateAppointmentStatus(String status)
            throws BillingException {
        if ("CANCELLED".equals(status)) {
            throw new BillingException(
                    BillingException.Reason.CANCELLED_APPOINTMENT,
                    "A cancelled appointment cannot be billed."
            );
        }
        if ("NO_SHOW".equals(status)) {
            throw new BillingException(
                    BillingException.Reason.INVALID_APPOINTMENT_STATUS,
                    "A no-show appointment cannot be billed."
            );
        }
        if (!"SCHEDULED".equals(status) && !"COMPLETED".equals(status)) {
            throw new BillingException(
                    BillingException.Reason.INVALID_APPOINTMENT_STATUS,
                    "This appointment status is not eligible for billing."
            );
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value, String fieldName)
            throws BillingException {
        if (value == null || value.signum() < 0) {
            throw invalidAmount(fieldName + " must be zero or greater.");
        }

        final BigDecimal normalized;
        try {
            normalized = value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BillingException(
                    BillingException.Reason.INVALID_AMOUNT,
                    fieldName + " must have no more than two decimal places.",
                    exception
            );
        }

        if (normalized.compareTo(MAX_DATABASE_AMOUNT) > 0) {
            throw invalidAmount(fieldName + " exceeds the supported amount.");
        }
        return normalized;
    }

    private String normalizeRequiredSingleLine(String value, String fieldName)
            throws BillingException {
        if (value == null) {
            throw validationError(fieldName + " is required.");
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            throw validationError(fieldName + " is required.");
        }
        return normalized;
    }

    private String formatPatientName(String firstName, String lastName) {
        return (firstName + " " + lastName).strip();
    }

    private BillingException translateDatabaseFailure(SQLException exception) {
        String databaseMessage = exception.getMessage() == null
                ? ""
                : exception.getMessage().toLowerCase(Locale.ROOT);
        if (databaseMessage.contains("uq_bills_appointment")) {
            return duplicateBill(exception);
        }
        if (databaseMessage.contains("chk_bills_")) {
            return new BillingException(
                    BillingException.Reason.INVALID_AMOUNT,
                    "The calculated bill amounts are invalid.",
                    exception
            );
        }
        return databaseFailure(exception);
    }

    private BillingException validationError(String message) {
        return new BillingException(BillingException.Reason.VALIDATION, message);
    }

    private BillingException invalidAmount(String message) {
        return new BillingException(
                BillingException.Reason.INVALID_AMOUNT,
                message
        );
    }

    private BillingException appointmentNotFound() {
        return new BillingException(
                BillingException.Reason.APPOINTMENT_NOT_FOUND,
                "No appointment was found with that appointment number."
        );
    }

    private BillingException duplicateBill() {
        return duplicateBill(null);
    }

    private BillingException duplicateBill(Throwable cause) {
        return new BillingException(
                BillingException.Reason.DUPLICATE_BILL,
                "A bill has already been generated for this appointment.",
                cause
        );
    }

    private BillingException databaseFailure(Throwable cause) {
        return new BillingException(
                BillingException.Reason.DATABASE_FAILURE,
                "The bill could not be processed because the database is "
                        + "currently unavailable. Please try again.",
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
