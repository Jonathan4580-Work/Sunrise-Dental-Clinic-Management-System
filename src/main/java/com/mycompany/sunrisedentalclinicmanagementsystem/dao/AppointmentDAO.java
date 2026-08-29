package com.mycompany.sunrisedentalclinicmanagementsystem.dao;

import com.mycompany.sunrisedentalclinicmanagementsystem.database.DatabaseConnection;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Appointment;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.AppointmentDetails;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Provides persistence operations required to register appointments.
 */
public final class AppointmentDAO {

    private static final String APPOINTMENT_NUMBER_EXISTS_SQL = """
            SELECT 1
            FROM appointments
            WHERE appointment_number = ?
            LIMIT 1
            """;

    private static final String DENTIST_CONFLICT_SQL = """
            SELECT 1
            FROM appointments
            WHERE dentist_id = ?
              AND appointment_date = ?
              AND appointment_time = ?
            LIMIT 1
            """;

    private static final String PATIENT_CONFLICT_SQL = """
            SELECT 1
            FROM appointments
            WHERE patient_id = ?
              AND appointment_date = ?
              AND appointment_time = ?
            LIMIT 1
            """;

    private static final String DENTIST_UPDATE_CONFLICT_SQL = """
            SELECT 1
            FROM appointments
            WHERE dentist_id = ?
              AND appointment_date = ?
              AND appointment_time = ?
              AND appointment_id <> ?
            LIMIT 1
            """;

    private static final String PATIENT_UPDATE_CONFLICT_SQL = """
            SELECT 1
            FROM appointments
            WHERE patient_id = ?
              AND appointment_date = ?
              AND appointment_time = ?
              AND appointment_id <> ?
            LIMIT 1
            """;

    private static final String APPOINTMENT_DETAILS_SELECT_SQL = """
            SELECT a.appointment_id,
                   a.appointment_number,
                   a.patient_id,
                   a.dentist_id,
                   a.treatment_id,
                   a.appointment_date,
                   a.appointment_time,
                   a.status,
                   a.notes,
                   a.updated_at,
                   p.patient_number,
                   p.first_name,
                   p.last_name,
                   p.address_line_1,
                   p.address_line_2,
                   p.city,
                   p.phone AS contact_number,
                   d.registration_number AS dentist_registration_number,
                   d.full_name AS dentist_name,
                   d.specialization AS dentist_specialization,
                   t.treatment_code,
                   t.treatment_name,
                   t.description AS treatment_description,
                   t.price AS treatment_price,
                   u.username AS created_by_username,
                   u.full_name AS created_by_full_name
            FROM appointments a
            INNER JOIN patients p ON p.patient_id = a.patient_id
            INNER JOIN dentists d ON d.dentist_id = a.dentist_id
            INNER JOIN treatments t ON t.treatment_id = a.treatment_id
            INNER JOIN users u ON u.user_id = a.created_by
            """;

    private static final String FIND_DETAILS_BY_NUMBER_SQL
            = APPOINTMENT_DETAILS_SELECT_SQL + """
            WHERE a.appointment_number = ?
            """;

    private static final String FIND_DETAILS_BY_ID_FOR_UPDATE_SQL
            = APPOINTMENT_DETAILS_SELECT_SQL + """
            WHERE a.appointment_id = ?
            FOR UPDATE
            """;

    private static final String INSERT_APPOINTMENT_SQL = """
            INSERT INTO appointments (
                appointment_number,
                patient_id,
                dentist_id,
                treatment_id,
                appointment_date,
                appointment_time,
                status,
                notes,
                created_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_APPOINTMENT_SQL = """
            UPDATE appointments
            SET dentist_id = ?,
                treatment_id = ?,
                appointment_date = ?,
                appointment_time = ?,
                notes = ?
            WHERE appointment_id = ?
            """;

    private static final String CANCEL_APPOINTMENT_SQL = """
            UPDATE appointments
            SET status = 'CANCELLED'
            WHERE appointment_id = ?
              AND status <> 'CANCELLED'
            """;

    /**
     * Finds the complete joined view for a unique appointment number.
     */
    public Optional<AppointmentDetails> findDetailsByAppointmentNumber(
            String appointmentNumber
    ) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        FIND_DETAILS_BY_NUMBER_SQL
                )) {
            statement.setString(1, appointmentNumber);
            return readAppointmentDetails(statement);
        }
    }

    /**
     * Reloads and locks an appointment while an external transaction updates it.
     */
    public Optional<AppointmentDetails> findDetailsByIdForUpdate(
            Connection connection,
            long appointmentId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                FIND_DETAILS_BY_ID_FOR_UPDATE_SQL
        )) {
            statement.setLong(1, appointmentId);
            return readAppointmentDetails(statement);
        }
    }

    public boolean appointmentNumberExists(String appointmentNumber)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return appointmentNumberExists(connection, appointmentNumber);
        }
    }

    public boolean appointmentNumberExists(
            Connection connection,
            String appointmentNumber
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                APPOINTMENT_NUMBER_EXISTS_SQL
        )) {
            statement.setString(1, appointmentNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean hasDentistScheduleConflict(
            long dentistId,
            LocalDate appointmentDate,
            LocalTime appointmentTime
    ) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return hasDentistScheduleConflict(
                    connection,
                    dentistId,
                    appointmentDate,
                    appointmentTime
            );
        }
    }

    public boolean hasDentistScheduleConflict(
            Connection connection,
            long dentistId,
            LocalDate appointmentDate,
            LocalTime appointmentTime
    ) throws SQLException {
        return exactScheduleExists(
                connection,
                DENTIST_CONFLICT_SQL,
                dentistId,
                appointmentDate,
                appointmentTime
        );
    }

    public boolean hasPatientScheduleConflict(
            long patientId,
            LocalDate appointmentDate,
            LocalTime appointmentTime
    ) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return hasPatientScheduleConflict(
                    connection,
                    patientId,
                    appointmentDate,
                    appointmentTime
            );
        }
    }

    public boolean hasPatientScheduleConflict(
            Connection connection,
            long patientId,
            LocalDate appointmentDate,
            LocalTime appointmentTime
    ) throws SQLException {
        return exactScheduleExists(
                connection,
                PATIENT_CONFLICT_SQL,
                patientId,
                appointmentDate,
                appointmentTime
        );
    }

    /**
     * Checks a dentist slot while excluding the appointment being edited.
     */
    public boolean hasDentistScheduleConflict(
            Connection connection,
            long dentistId,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            long excludedAppointmentId
    ) throws SQLException {
        return exactScheduleExistsExcludingAppointment(
                connection,
                DENTIST_UPDATE_CONFLICT_SQL,
                dentistId,
                appointmentDate,
                appointmentTime,
                excludedAppointmentId
        );
    }

    /**
     * Checks a patient slot while excluding the appointment being edited.
     */
    public boolean hasPatientScheduleConflict(
            Connection connection,
            long patientId,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            long excludedAppointmentId
    ) throws SQLException {
        return exactScheduleExistsExcludingAppointment(
                connection,
                PATIENT_UPDATE_CONFLICT_SQL,
                patientId,
                appointmentDate,
                appointmentTime,
                excludedAppointmentId
        );
    }

    public long insert(Appointment appointment) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return insert(connection, appointment);
        }
    }

    /**
     * Inserts an appointment without managing the supplied transaction.
     */
    public long insert(Connection connection, Appointment appointment)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_APPOINTMENT_SQL,
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, appointment.getAppointmentNumber());
            statement.setLong(2, appointment.getPatientId());
            statement.setLong(3, appointment.getDentistId());
            statement.setLong(4, appointment.getTreatmentId());
            statement.setDate(5, Date.valueOf(appointment.getAppointmentDate()));
            statement.setTime(6, Time.valueOf(appointment.getAppointmentTime()));
            statement.setString(7, appointment.getStatus());
            statement.setString(8, appointment.getNotes());
            statement.setLong(9, appointment.getCreatedBy());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException(
                            "Appointment was inserted, but no generated ID was returned."
                    );
                }

                long appointmentId = generatedKeys.getLong(1);
                appointment.setAppointmentId(appointmentId);
                return appointmentId;
            }
        }
    }

    /**
     * Updates only the editable scheduling fields in an external transaction.
     */
    public boolean updateAppointment(
            Connection connection,
            Appointment appointment
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                UPDATE_APPOINTMENT_SQL
        )) {
            statement.setLong(1, appointment.getDentistId());
            statement.setLong(2, appointment.getTreatmentId());
            statement.setDate(3, Date.valueOf(appointment.getAppointmentDate()));
            statement.setTime(4, Time.valueOf(appointment.getAppointmentTime()));
            statement.setString(5, appointment.getNotes());
            statement.setLong(6, appointment.getAppointmentId());
            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Soft-cancels a non-cancelled appointment without deleting its history.
     */
    public boolean cancelAppointment(Connection connection, long appointmentId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                CANCEL_APPOINTMENT_SQL
        )) {
            statement.setLong(1, appointmentId);
            return statement.executeUpdate() == 1;
        }
    }

    private boolean exactScheduleExists(
            Connection connection,
            String sql,
            long entityId,
            LocalDate appointmentDate,
            LocalTime appointmentTime
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, entityId);
            statement.setDate(2, Date.valueOf(appointmentDate));
            statement.setTime(3, Time.valueOf(appointmentTime));

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean exactScheduleExistsExcludingAppointment(
            Connection connection,
            String sql,
            long entityId,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            long excludedAppointmentId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, entityId);
            statement.setDate(2, Date.valueOf(appointmentDate));
            statement.setTime(3, Time.valueOf(appointmentTime));
            statement.setLong(4, excludedAppointmentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private Optional<AppointmentDetails> readAppointmentDetails(
            PreparedStatement statement
    ) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return Optional.empty();
            }

            return Optional.of(new AppointmentDetails(
                    resultSet.getLong("appointment_id"),
                    resultSet.getLong("patient_id"),
                    resultSet.getLong("dentist_id"),
                    resultSet.getLong("treatment_id"),
                    resultSet.getString("appointment_number"),
                    resultSet.getString("patient_number"),
                    resultSet.getString("first_name"),
                    resultSet.getString("last_name"),
                    resultSet.getString("address_line_1"),
                    resultSet.getString("address_line_2"),
                    resultSet.getString("city"),
                    resultSet.getString("contact_number"),
                    resultSet.getString("dentist_registration_number"),
                    resultSet.getString("dentist_name"),
                    resultSet.getString("dentist_specialization"),
                    resultSet.getString("treatment_code"),
                    resultSet.getString("treatment_name"),
                    resultSet.getString("treatment_description"),
                    resultSet.getBigDecimal("treatment_price"),
                    resultSet.getDate("appointment_date").toLocalDate(),
                    resultSet.getTime("appointment_time").toLocalTime(),
                    resultSet.getString("status"),
                    resultSet.getString("notes"),
                    resultSet.getString("created_by_username"),
                    resultSet.getString("created_by_full_name"),
                    resultSet.getTimestamp("updated_at").toLocalDateTime()
            ));
        }
    }
}
