package com.mycompany.sunrisedentalclinicmanagementsystem.dao;

import com.mycompany.sunrisedentalclinicmanagementsystem.database.DatabaseConnection;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Dentist;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides read access to dentist records used by appointment registration.
 */
public final class DentistDAO {

    private static final String FIND_ACTIVE_DENTISTS_SQL = """
            SELECT dentist_id,
                   registration_number,
                   full_name,
                   specialization,
                   is_active
            FROM dentists
            WHERE is_active = TRUE
            ORDER BY full_name ASC
            """;

    private static final String IS_ACTIVE_DENTIST_SQL = """
            SELECT 1
            FROM dentists
            WHERE dentist_id = ?
              AND is_active = TRUE
            LIMIT 1
            """;

    public List<Dentist> findAllActive() throws SQLException {
        List<Dentist> dentists = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        FIND_ACTIVE_DENTISTS_SQL
                );
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                dentists.add(new Dentist(
                        resultSet.getLong("dentist_id"),
                        resultSet.getString("registration_number"),
                        resultSet.getString("full_name"),
                        resultSet.getString("specialization"),
                        resultSet.getBoolean("is_active")
                ));
            }
        }

        return dentists;
    }

    /**
     * Checks a dentist selection within an externally managed transaction.
     */
    public boolean isActive(Connection connection, long dentistId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                IS_ACTIVE_DENTIST_SQL
        )) {
            statement.setLong(1, dentistId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
