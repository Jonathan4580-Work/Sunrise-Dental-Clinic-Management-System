package com.mycompany.sunrisedentalclinicmanagementsystem.dao;

import com.mycompany.sunrisedentalclinicmanagementsystem.database.DatabaseConnection;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Appointment;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

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
}
