package com.mycompany.sunrisedentalclinicmanagementsystem.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Creates JDBC connections to the Sunrise Dental Clinic database.
 */
public final class DatabaseConnection {

    private static final String DATABASE_URL
            = "jdbc:mysql://localhost:3306/dental_clinic_db"
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Colombo";
    private static final String DATABASE_USERNAME = "root";

    // Local XAMPP development default. Externalize before deployment.
    private static final String DATABASE_PASSWORD = "";

    private DatabaseConnection() {
        // This utility class provides connections through its static method.
    }

    /**
     * Opens a new connection to the clinic database.
     *
     * <p>The caller owns the returned connection and must close it, preferably
     * with a try-with-resources statement.</p>
     *
     * @return an open JDBC connection
     * @throws SQLException if the database cannot be reached or rejects the
     *         configured credentials
     */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(
                    DATABASE_URL,
                    DATABASE_USERNAME,
                    DATABASE_PASSWORD
            );
        } catch (SQLException exception) {
            throw new SQLException(
                    "Unable to connect to dental_clinic_db on localhost:3306. "
                            + "Confirm that XAMPP MySQL is running and the "
                            + "local database credentials are correct.",
                    exception.getSQLState(),
                    exception.getErrorCode(),
                    exception
            );
        }
    }
}
