package com.mycompany.sunrisedentalclinicmanagementsystem.dao;

import com.mycompany.sunrisedentalclinicmanagementsystem.database.DatabaseConnection;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Bill;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Provides persistence operations required to generate clinic bills.
 */
public final class BillDAO {

    private static final String BILL_EXISTS_FOR_APPOINTMENT_SQL = """
            SELECT 1
            FROM bills
            WHERE appointment_id = ?
            LIMIT 1
            """;

    private static final String INSERT_BILL_SQL = """
            INSERT INTO bills (
                bill_number,
                appointment_id,
                subtotal,
                discount_amount,
                tax_amount,
                amount_paid,
                payment_status,
                payment_method,
                paid_at,
                generated_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    public boolean existsForAppointment(long appointmentId)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return existsForAppointment(connection, appointmentId);
        }
    }

    /**
     * Checks for an existing bill inside an externally managed transaction.
     */
    public boolean existsForAppointment(
            Connection connection,
            long appointmentId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                BILL_EXISTS_FOR_APPOINTMENT_SQL
        )) {
            statement.setLong(1, appointmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public long insert(Bill bill) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return insert(connection, bill);
        }
    }

    /**
     * Inserts a bill without committing or closing the supplied connection.
     */
    public long insert(Connection connection, Bill bill) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_BILL_SQL,
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, bill.getBillNumber());
            statement.setLong(2, bill.getAppointmentId());
            statement.setBigDecimal(3, bill.getSubtotal());
            statement.setBigDecimal(4, bill.getDiscountAmount());
            statement.setBigDecimal(5, bill.getTaxAmount());
            statement.setBigDecimal(6, bill.getAmountPaid());
            statement.setString(7, bill.getPaymentStatus());
            setNullableString(statement, 8, bill.getPaymentMethod());
            setNullableTimestamp(statement, 9, bill.getPaidAt());
            statement.setLong(10, bill.getGeneratedBy());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException(
                            "Bill was inserted, but no generated ID was returned."
                    );
                }

                long billId = generatedKeys.getLong(1);
                bill.setBillId(billId);
                return billId;
            }
        }
    }

    private void setNullableString(
            PreparedStatement statement,
            int parameterIndex,
            String value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.VARCHAR);
            return;
        }
        statement.setString(parameterIndex, value);
    }

    private void setNullableTimestamp(
            PreparedStatement statement,
            int parameterIndex,
            java.time.LocalDateTime value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.TIMESTAMP);
            return;
        }
        statement.setTimestamp(parameterIndex, Timestamp.valueOf(value));
    }
}
