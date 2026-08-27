package com.mycompany.sunrisedentalclinicmanagementsystem.dao;

import com.mycompany.sunrisedentalclinicmanagementsystem.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalLong;

/**
 * Provides the user lookup needed to attribute newly created records.
 */
public final class UserDAO {

    private static final String FIND_ACTIVE_USER_ID_SQL = """
            SELECT user_id
            FROM users
            WHERE username = ?
              AND is_active = TRUE
            LIMIT 1
            """;

    public OptionalLong findActiveUserIdByUsername(String username)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return findActiveUserIdByUsername(connection, username);
        }
    }

    /**
     * Finds an active user inside an externally managed transaction.
     */
    public OptionalLong findActiveUserIdByUsername(
            Connection connection,
            String username
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                FIND_ACTIVE_USER_ID_SQL
        )) {
            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return OptionalLong.of(resultSet.getLong("user_id"));
                }
                return OptionalLong.empty();
            }
        }
    }
}
