package com.mycompany.sunrisedentalclinicmanagementsystem.dao;

import com.mycompany.sunrisedentalclinicmanagementsystem.database.DatabaseConnection;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Treatment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides read access to active treatment catalogue records.
 */
public final class TreatmentDAO {

    private static final String FIND_ACTIVE_TREATMENTS_SQL = """
            SELECT treatment_id,
                   treatment_code,
                   treatment_name,
                   description,
                   price,
                   is_active
            FROM treatments
            WHERE is_active = TRUE
            ORDER BY treatment_name ASC
            """;

    private static final String IS_ACTIVE_TREATMENT_SQL = """
            SELECT 1
            FROM treatments
            WHERE treatment_id = ?
              AND is_active = TRUE
            LIMIT 1
            """;

    private static final String FIND_ACTIVE_TREATMENT_BY_CODE_SQL = """
            SELECT treatment_id,
                   treatment_code,
                   treatment_name,
                   description,
                   price,
                   is_active
            FROM treatments
            WHERE treatment_code = ?
              AND is_active = TRUE
            LIMIT 1
            """;

    public List<Treatment> findAllActive() throws SQLException {
        List<Treatment> treatments = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        FIND_ACTIVE_TREATMENTS_SQL
                );
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                treatments.add(new Treatment(
                        resultSet.getLong("treatment_id"),
                        resultSet.getString("treatment_code"),
                        resultSet.getString("treatment_name"),
                        resultSet.getString("description"),
                        resultSet.getBigDecimal("price"),
                        resultSet.getBoolean("is_active")
                ));
            }
        }

        return treatments;
    }

    /**
     * Checks a treatment selection within an externally managed transaction.
     */
    public boolean isActive(Connection connection, long treatmentId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                IS_ACTIVE_TREATMENT_SQL
        )) {
            statement.setLong(1, treatmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * Finds an active catalogue treatment by its stable public code.
     */
    public Optional<Treatment> findActiveByCode(
            Connection connection,
            String treatmentCode
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                FIND_ACTIVE_TREATMENT_BY_CODE_SQL
        )) {
            statement.setString(1, treatmentCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(new Treatment(
                        resultSet.getLong("treatment_id"),
                        resultSet.getString("treatment_code"),
                        resultSet.getString("treatment_name"),
                        resultSet.getString("description"),
                        resultSet.getBigDecimal("price"),
                        resultSet.getBoolean("is_active")
                ));
            }
        }
    }
}
