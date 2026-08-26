package com.mycompany.sunrisedentalclinicmanagementsystem.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Standalone development check for the JDBC connection configuration.
 */
public final class DatabaseConnectionTest {

    private DatabaseConnectionTest() {
        // Prevent creation of this test utility class.
    }

    /**
     * Attempts to open and validate one database connection.
     *
     * @param args command-line arguments are not used
     */
    public static void main(String[] args) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            if (connection.isValid(5)) {
                System.out.println("Database connection successful.");
                return;
            }

            System.err.println("Database connection failed: "
                    + "the server did not validate the connection.");
        } catch (SQLException exception) {
            System.err.println("Database connection failed: "
                    + exception.getMessage());
        }
    }
}
