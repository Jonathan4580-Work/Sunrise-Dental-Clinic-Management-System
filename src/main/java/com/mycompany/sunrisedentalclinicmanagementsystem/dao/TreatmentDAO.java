package com.mycompany.sunrisedentalclinicmanagementsystem.dao;

import com.mycompany.sunrisedentalclinicmanagementsystem.database.DatabaseConnection;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Treatment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
}
