package com.mycompany.sunrisedentalclinicmanagementsystem.dao;

import com.mycompany.sunrisedentalclinicmanagementsystem.database.DatabaseConnection;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Patient;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/**
 * Provides persistence operations for patient records.
 */
public final class PatientDAO {

    private static final String INSERT_PATIENT_SQL = """
            INSERT INTO patients (
                patient_number,
                first_name,
                last_name,
                date_of_birth,
                gender,
                phone,
                email,
                national_id,
                address_line_1,
                address_line_2,
                city,
                medical_notes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_PATIENT_SQL = """
            UPDATE patients
            SET first_name = ?,
                last_name = ?,
                address_line_1 = ?,
                address_line_2 = ?,
                city = ?,
                phone = ?
            WHERE patient_id = ?
            """;

    /**
     * Inserts a patient using a connection owned by this method.
     *
     * @param patient patient to insert
     * @return generated patient ID
     * @throws SQLException if the insert fails
     */
    public long insert(Patient patient) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return insert(connection, patient);
        }
    }

    /**
     * Inserts a patient using an externally managed connection.
     *
     * <p>This method does not close, commit, or roll back the supplied
     * connection.</p>
     *
     * @param connection transaction connection
     * @param patient patient to insert
     * @return generated patient ID
     * @throws SQLException if the insert fails or no key is returned
     */
    public long insert(Connection connection, Patient patient) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_PATIENT_SQL,
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, patient.getPatientNumber());
            statement.setString(2, patient.getFirstName());
            statement.setString(3, patient.getLastName());
            setNullableDate(statement, 4, patient.getDateOfBirth());
            statement.setString(5, patient.getGender());
            statement.setString(6, patient.getPhone());
            statement.setString(7, patient.getEmail());
            statement.setString(8, patient.getNationalId());
            statement.setString(9, patient.getAddressLine1());
            statement.setString(10, patient.getAddressLine2());
            statement.setString(11, patient.getCity());
            statement.setString(12, patient.getMedicalNotes());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException(
                            "Patient was inserted, but no generated ID was returned."
                    );
                }

                long patientId = generatedKeys.getLong(1);
                patient.setPatientId(patientId);
                return patientId;
            }
        }
    }

    /**
     * Updates only appointment-management patient fields in an external
     * transaction.
     */
    public boolean update(Connection connection, Patient patient)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                UPDATE_PATIENT_SQL
        )) {
            statement.setString(1, patient.getFirstName());
            statement.setString(2, patient.getLastName());
            statement.setString(3, patient.getAddressLine1());
            statement.setString(4, patient.getAddressLine2());
            statement.setString(5, patient.getCity());
            statement.setString(6, patient.getPhone());
            statement.setLong(7, patient.getPatientId());
            return statement.executeUpdate() == 1;
        }
    }

    private void setNullableDate(
            PreparedStatement statement,
            int parameterIndex,
            java.time.LocalDate value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.DATE);
            return;
        }

        statement.setDate(parameterIndex, Date.valueOf(value));
    }
}
