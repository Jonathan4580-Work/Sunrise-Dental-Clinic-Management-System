package com.mycompany.sunrisedentalclinicmanagementsystem.model;

/**
 * Immutable result of a successful bill-generation transaction.
 *
 * <p>The calculation is the authoritative breakdown used to create the
 * accompanying stored bill.</p>
 */
public record GeneratedBill(
        Bill bill,
        BillingCalculation calculation
) {
}
