package com.mycompany.sunrisedentalclinicmanagementsystem.controller;

import com.mycompany.sunrisedentalclinicmanagementsystem.model.Bill;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.BillingCalculation;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.GeneratedBill;
import com.mycompany.sunrisedentalclinicmanagementsystem.service.BillingException;
import com.mycompany.sunrisedentalclinicmanagementsystem.service.BillingService;
import com.mycompany.sunrisedentalclinicmanagementsystem.ui.BillingFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.awt.print.PrinterException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Coordinates bill calculation, persistence, and receipt-preview state.
 */
public final class BillingController {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Colombo");
    private static final DateTimeFormatter APPOINTMENT_DATE_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter APPOINTMENT_TIME_FORMAT
            = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter GENERATED_AT_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
    private static final String RECEIPT_SEPARATOR
            = "--------------------------------------------------";

    private final BillingFrame frame;
    private final BillingService billingService;
    private final String authenticatedUsername;
    private final Runnable returnToDashboardAction;

    private BillingCalculation currentCalculation;
    private GeneratedBill currentGeneratedBill;
    private boolean returningToDashboard;

    /**
     * Creates the billing feature for the authenticated staff member.
     */
    public BillingController(
            String authenticatedUsername,
            Runnable returnToDashboardAction
    ) {
        this(
                new BillingFrame(),
                new BillingService(),
                authenticatedUsername,
                returnToDashboardAction
        );
    }

    BillingController(
            BillingFrame frame,
            BillingService billingService,
            String authenticatedUsername,
            Runnable returnToDashboardAction
    ) {
        this.frame = Objects.requireNonNull(frame, "frame must not be null");
        this.billingService = Objects.requireNonNull(
                billingService,
                "billingService must not be null"
        );
        this.authenticatedUsername = Objects.requireNonNull(
                authenticatedUsername,
                "authenticatedUsername must not be null"
        );
        this.returnToDashboardAction = Objects.requireNonNull(
                returnToDashboardAction,
                "returnToDashboardAction must not be null"
        );
        registerEvents();
    }

    /**
     * Displays the billing frame on Swing's Event Dispatch Thread.
     */
    public void show() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::show);
            return;
        }
        frame.setVisible(true);
    }

    private void registerEvents() {
        frame.addSearchListener(event -> calculateBill());
        frame.addGenerateBillListener(event -> generateBill());
        frame.addClearListener(event -> clear());
        frame.addBackListener(event -> returnToDashboard());
        frame.addPrintReceiptListener(event -> printReceipt());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                returnToDashboard();
            }
        });
    }

    private void calculateBill() {
        currentCalculation = null;
        currentGeneratedBill = null;
        frame.clearDetails();
        frame.setReceiptText(receiptLoadingMessage());
        frame.setBusy(true, "Searching appointment and calculating bill...");
        String appointmentNumber = frame.getSearchAppointmentNumber();

        new SwingWorker<BillingCalculation, Void>() {
            @Override
            protected BillingCalculation doInBackground()
                    throws BillingException {
                return billingService.calculateBill(appointmentNumber);
            }

            @Override
            protected void done() {
                try {
                    currentCalculation = get();
                    frame.displayCalculation(currentCalculation);
                    frame.setReceiptText(buildDraftReceipt(currentCalculation));
                    frame.setBusy(false, "Bill calculation ready.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showInterruptedFailure("Bill calculation interrupted.");
                } catch (ExecutionException exception) {
                    currentCalculation = null;
                    frame.clearDetails();
                    frame.setReceiptText(receiptUnavailableMessage());
                    frame.setBusy(false, "Unable to calculate bill.");
                    handleFailure(exception.getCause());
                }
            }
        }.execute();
    }

    private void generateBill() {
        if (currentCalculation == null) {
            return;
        }

        String appointmentNumber = currentCalculation.appointmentNumber();
        frame.setBusy(true, "Generating bill...");

        new SwingWorker<GeneratedBill, Void>() {
            @Override
            protected GeneratedBill doInBackground() throws BillingException {
                return billingService.createBill(
                        appointmentNumber,
                        authenticatedUsername
                );
            }

            @Override
            protected void done() {
                try {
                    GeneratedBill generatedBill = get();
                    currentGeneratedBill = generatedBill;
                    currentCalculation = generatedBill.calculation();
                    frame.displayCalculation(currentCalculation);
                    frame.markBillGenerated();
                    frame.setReceiptText(buildFinalReceipt(generatedBill));
                    frame.setBusy(false, "Bill generated successfully.");
                    frame.showSuccessMessage(
                            "Bill generated successfully.\n\n"
                                    + "Bill Number: "
                                    + generatedBill.bill().getBillNumber()
                    );
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showInterruptedFailure("Bill generation interrupted.");
                } catch (ExecutionException exception) {
                    frame.setBusy(false, "Bill generation failed.");
                    handleFailure(exception.getCause());
                }
            }
        }.execute();
    }

    private void clear() {
        currentCalculation = null;
        currentGeneratedBill = null;
        frame.clearForm();
    }

    private void printReceipt() {
        if (currentGeneratedBill == null) {
            return;
        }

        frame.setBusy(true, "Opening print dialog...");
        try {
            boolean printed = frame.printReceipt();
            if (printed) {
                frame.setBusy(false, "Receipt sent to the printer.");
                frame.showInformationMessage(
                        "Receipt Printing",
                        "The receipt was sent to the selected printer."
                );
                return;
            }

            frame.setBusy(false, "Receipt printing cancelled.");
            frame.showInformationMessage(
                    "Printing Cancelled",
                    "Receipt printing was cancelled. No billing data was changed."
            );
        } catch (PrinterException exception) {
            frame.setBusy(false, "Unable to print receipt.");
            frame.showErrorMessage(
                    "Printing Failed",
                    "The receipt could not be printed. Check the selected "
                            + "printer and try again."
            );
        }
    }

    private void handleFailure(Throwable failure) {
        if (failure instanceof BillingException exception) {
            if (exception.getReason() == BillingException.Reason.DUPLICATE_BILL) {
                frame.markDuplicateBill();
                frame.showInformationMessage(
                        "Bill Already Exists",
                        exception.getMessage()
                );
                return;
            }

            frame.showErrorMessage(
                    errorTitle(exception.getReason()),
                    exception.getMessage()
            );
            return;
        }

        frame.showErrorMessage(
                "Billing Operation Failed",
                "The billing operation could not be completed. Please try again."
        );
    }

    private String errorTitle(BillingException.Reason reason) {
        return switch (reason) {
            case VALIDATION -> "Check Appointment Number";
            case APPOINTMENT_NOT_FOUND -> "Appointment Not Found";
            case CANCELLED_APPOINTMENT -> "Cancelled Appointment";
            case INVALID_APPOINTMENT_STATUS -> "Appointment Cannot Be Billed";
            case DUPLICATE_BILL -> "Bill Already Exists";
            case INACTIVE_TREATMENT -> "Treatment Unavailable";
            case CONSULTATION_FEE_UNAVAILABLE -> "Consultation Fee Unavailable";
            case INACTIVE_USER -> "Staff Account Unavailable";
            case INVALID_AMOUNT -> "Invalid Bill Amount";
            case DATABASE_FAILURE -> "Database Unavailable";
        };
    }

    private String buildDraftReceipt(BillingCalculation calculation) {
        StringBuilder receipt = new StringBuilder();
        appendCentered(receipt, "SUNRISE DENTAL CLINIC");
        appendCentered(receipt, "BILL CALCULATION PREVIEW");
        receipt.append(RECEIPT_SEPARATOR).append('\n');
        appendLine(receipt, "Bill Number", "Not generated");
        appendLine(receipt, "Payment Status", "NOT GENERATED");
        receipt.append(RECEIPT_SEPARATOR).append('\n');
        appendAppointmentInformation(receipt, calculation);
        appendCharges(receipt, calculation);
        receipt.append('\n');
        appendCentered(receipt, "Review the charges before generating the bill.");
        return receipt.toString();
    }

    private String buildFinalReceipt(GeneratedBill generatedBill) {
        Bill bill = generatedBill.bill();
        BillingCalculation calculation = generatedBill.calculation();
        LocalDateTime generatedAt = bill.getCreatedAt() == null
                ? LocalDateTime.now(CLINIC_ZONE)
                : bill.getCreatedAt();

        StringBuilder receipt = new StringBuilder();
        appendCentered(receipt, "SUNRISE DENTAL CLINIC");
        appendCentered(receipt, "PATIENT BILL / RECEIPT");
        receipt.append(RECEIPT_SEPARATOR).append('\n');
        appendLine(receipt, "Bill Number", bill.getBillNumber());
        appendLine(
                receipt,
                "Generated",
                generatedAt.format(GENERATED_AT_FORMAT)
        );
        appendLine(receipt, "Generated By", authenticatedUsername);
        appendLine(receipt, "Payment Status", bill.getPaymentStatus());
        receipt.append(RECEIPT_SEPARATOR).append('\n');
        appendAppointmentInformation(receipt, calculation);
        appendCharges(receipt, calculation);
        receipt.append('\n');
        appendCentered(receipt, "Thank you for choosing Sunrise Dental Clinic.");
        return receipt.toString();
    }

    private void appendAppointmentInformation(
            StringBuilder receipt,
            BillingCalculation calculation
    ) {
        appendLine(receipt, "Appointment", calculation.appointmentNumber());
        appendLine(receipt, "Patient Number", calculation.patientNumber());
        appendLine(receipt, "Patient", calculation.patientName());
        appendLine(receipt, "Contact", calculation.contactNumber());
        appendLine(receipt, "Dentist", calculation.dentistName());
        appendLine(receipt, "Treatment", calculation.treatmentName());
        appendLine(
                receipt,
                "Appointment Date",
                calculation.appointmentDate().format(APPOINTMENT_DATE_FORMAT)
        );
        appendLine(
                receipt,
                "Appointment Time",
                calculation.appointmentTime().format(APPOINTMENT_TIME_FORMAT)
        );
        receipt.append(RECEIPT_SEPARATOR).append('\n');
    }

    private void appendCharges(
            StringBuilder receipt,
            BillingCalculation calculation
    ) {
        appendLine(
                receipt,
                "Treatment Price",
                formatCurrency(calculation.treatmentPrice())
        );
        appendLine(
                receipt,
                "Consultation Fee",
                formatCurrency(calculation.consultationFee())
        );
        appendLine(receipt, "Subtotal", formatCurrency(calculation.subtotal()));
        appendLine(
                receipt,
                "Discount",
                formatCurrency(calculation.discountAmount())
        );
        appendLine(receipt, "Tax", formatCurrency(calculation.taxAmount()));
        receipt.append(RECEIPT_SEPARATOR).append('\n');
        appendLine(
                receipt,
                "FINAL TOTAL",
                formatCurrency(calculation.totalAmount())
        );
        receipt.append(RECEIPT_SEPARATOR).append('\n');
    }

    private void appendLine(
            StringBuilder receipt,
            String label,
            String value
    ) {
        receipt.append(String.format(
                Locale.ROOT,
                "%-21s %28s%n",
                label + ":",
                value == null ? "" : value
        ));
    }

    private void appendCentered(StringBuilder receipt, String text) {
        int padding = Math.max(0, (50 - text.length()) / 2);
        receipt.append(" ".repeat(padding)).append(text).append('\n');
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format(Locale.ROOT, "LKR %,.2f", amount);
    }

    private String receiptLoadingMessage() {
        return "\n\n\n"
                + "             Calculating bill preview...";
    }

    private String receiptUnavailableMessage() {
        return "\n\n\n"
                + "        Receipt preview is currently unavailable.";
    }

    private void showInterruptedFailure(String status) {
        frame.setBusy(false, status);
        frame.showErrorMessage(
                "Operation Interrupted",
                "The billing operation was interrupted. Please try again."
        );
    }

    private void returnToDashboard() {
        if (returningToDashboard) {
            return;
        }

        returningToDashboard = true;
        frame.dispose();
        returnToDashboardAction.run();
    }

}
