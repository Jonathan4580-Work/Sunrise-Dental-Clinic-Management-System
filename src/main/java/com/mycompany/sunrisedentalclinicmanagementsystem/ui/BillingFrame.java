package com.mycompany.sunrisedentalclinicmanagementsystem.ui;

import com.mycompany.sunrisedentalclinicmanagementsystem.model.BillingCalculation;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 * Read-only billing workspace used to calculate and generate patient bills.
 */
public final class BillingFrame extends JFrame {

    private static final Color PRIMARY_BLUE = new Color(22, 91, 170);
    private static final Color DARK_BLUE = new Color(14, 55, 102);
    private static final Color TEXT_COLOR = new Color(37, 50, 66);
    private static final Color MUTED_TEXT_COLOR = new Color(105, 118, 132);
    private static final Color WINDOW_BACKGROUND = new Color(242, 247, 252);
    private static final Color CARD_BORDER_COLOR = new Color(218, 226, 234);
    private static final Color FIELD_BORDER_COLOR = new Color(198, 211, 224);
    private static final Color READ_ONLY_BACKGROUND = new Color(246, 249, 252);
    private static final Color SUCCESS_COLOR = new Color(31, 132, 83);
    private static final Color ERROR_COLOR = new Color(190, 55, 55);

    private static final Font HEADER_TITLE_FONT
            = new Font("SansSerif", Font.BOLD, 25);
    private static final Font PAGE_TITLE_FONT
            = new Font("SansSerif", Font.BOLD, 20);
    private static final Font SECTION_TITLE_FONT
            = new Font("SansSerif", Font.BOLD, 17);
    private static final Font LABEL_FONT
            = new Font("SansSerif", Font.BOLD, 13);
    private static final Font INPUT_FONT
            = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font BUTTON_FONT
            = new Font("SansSerif", Font.BOLD, 13);
    private static final Font RECEIPT_FONT
            = new Font("Monospaced", Font.PLAIN, 13);

    private static final DateTimeFormatter DATE_FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER
            = DateTimeFormatter.ofPattern("hh:mm a");

    private JTextField searchField;
    private JLabel statusLabel;
    private JButton searchButton;

    private JTextField appointmentNumberField;
    private JTextField patientNumberField;
    private JTextField patientNameField;
    private JTextField contactNumberField;
    private JTextField dentistField;
    private JTextField treatmentField;
    private JTextField appointmentDateField;
    private JTextField appointmentTimeField;
    private JTextField appointmentStatusField;

    private JTextField treatmentPriceField;
    private JTextField consultationFeeField;
    private JTextField subtotalField;
    private JTextField discountField;
    private JTextField taxField;
    private JTextField finalTotalField;

    private JTextArea receiptTextArea;
    private JButton generateBillButton;
    private JButton clearButton;
    private JButton backButton;
    private JButton printReceiptButton;

    private boolean calculationLoaded;
    private boolean billGenerated;
    private boolean receiptPrintable;
    private boolean busy;

    public BillingFrame() {
        initializeComponents();
        setupLayout();
        clearForm();
        pack();
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        setTitle("Sunrise Dental Clinic Management System - Billing");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setIconImage(createApplicationIcon());

        searchField = createTextField();
        searchButton = createPrimaryButton("Search / Calculate", 170);
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusLabel.setForeground(MUTED_TEXT_COLOR);

        appointmentNumberField = createReadOnlyField();
        patientNumberField = createReadOnlyField();
        patientNameField = createReadOnlyField();
        contactNumberField = createReadOnlyField();
        dentistField = createReadOnlyField();
        treatmentField = createReadOnlyField();
        appointmentDateField = createReadOnlyField();
        appointmentTimeField = createReadOnlyField();
        appointmentStatusField = createReadOnlyField();

        treatmentPriceField = createMoneyField();
        consultationFeeField = createMoneyField();
        subtotalField = createMoneyField();
        discountField = createMoneyField();
        taxField = createMoneyField();
        finalTotalField = createMoneyField();
        finalTotalField.setFont(new Font("SansSerif", Font.BOLD, 17));
        finalTotalField.setForeground(DARK_BLUE);

        receiptTextArea = new JTextArea(31, 42);
        receiptTextArea.setEditable(false);
        receiptTextArea.setFont(RECEIPT_FONT);
        receiptTextArea.setForeground(TEXT_COLOR);
        receiptTextArea.setBackground(new Color(252, 253, 254));
        receiptTextArea.setMargin(new Insets(16, 18, 16, 18));
        receiptTextArea.setLineWrap(false);

        generateBillButton = createPrimaryButton("Generate Bill", 145);
        clearButton = createSecondaryButton("Clear", 105);
        backButton = createSecondaryButton("Back to Dashboard", 170);
        printReceiptButton = createSecondaryButton("Print Receipt", 135);
        printReceiptButton.setToolTipText(
                "Generate a bill before printing its final receipt."
        );
    }

    private void setupLayout() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setPreferredSize(new Dimension(1260, 800));
        rootPanel.setBackground(WINDOW_BACKGROUND);
        rootPanel.add(createHeader(), BorderLayout.NORTH);

        JPanel workspacePanel = new JPanel(new BorderLayout(0, 16));
        workspacePanel.setOpaque(false);
        workspacePanel.setBorder(new EmptyBorder(16, 20, 16, 20));
        workspacePanel.add(createSearchPanel(), BorderLayout.NORTH);
        workspacePanel.add(createMainContent(), BorderLayout.CENTER);

        rootPanel.add(workspacePanel, BorderLayout.CENTER);
        rootPanel.add(createFooter(), BorderLayout.SOUTH);
        setContentPane(rootPanel);
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(DARK_BLUE);
        headerPanel.setBorder(new EmptyBorder(17, 24, 17, 24));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel clinicLabel = new JLabel(
                "Sunrise Dental Clinic Management System"
        );
        clinicLabel.setFont(HEADER_TITLE_FONT);
        clinicLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Calculate Patient Bill");
        titleLabel.setFont(PAGE_TITLE_FONT);
        titleLabel.setForeground(new Color(225, 238, 252));

        JLabel subtitleLabel = new JLabel(
                "Review appointment charges and generate a patient bill."
        );
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(195, 218, 242));

        headerPanel.add(clinicLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(3));
        headerPanel.add(subtitleLabel);
        return headerPanel;
    }

    private JPanel createSearchPanel() {
        JPanel panel = createCardPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(0, 0, 0, 14);
        panel.add(createFormLabel("Appointment Number"), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = 0;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(0, 0, 0, 14);
        panel.add(searchField, fieldConstraints);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 2;
        buttonConstraints.gridy = 0;
        panel.add(searchButton, buttonConstraints);

        GridBagConstraints statusConstraints = new GridBagConstraints();
        statusConstraints.gridx = 1;
        statusConstraints.gridy = 1;
        statusConstraints.gridwidth = 2;
        statusConstraints.anchor = GridBagConstraints.WEST;
        statusConstraints.insets = new Insets(8, 0, 0, 0);
        panel.add(statusLabel, statusConstraints);
        return panel;
    }

    private JPanel createMainContent() {
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        mainPanel.setOpaque(false);

        JPanel detailsColumn = new JPanel();
        detailsColumn.setOpaque(false);
        detailsColumn.setLayout(new BoxLayout(detailsColumn, BoxLayout.Y_AXIS));
        detailsColumn.add(createAppointmentDetailsCard());
        detailsColumn.add(Box.createVerticalStrut(14));
        detailsColumn.add(createCalculationCard());

        mainPanel.add(detailsColumn);
        mainPanel.add(createReceiptCard());
        return mainPanel;
    }

    private JPanel createAppointmentDetailsCard() {
        JPanel card = createSectionCard("Patient / Appointment Information");
        addFormRow(card, 0, 0, "Appointment Number", appointmentNumberField);
        addFormRow(card, 0, 1, "Patient Number", patientNumberField);
        addFormRow(card, 1, 0, "Patient Name", patientNameField);
        addFormRow(card, 1, 1, "Contact Number", contactNumberField);
        addFormRow(card, 2, 0, "Dentist", dentistField);
        addFormRow(card, 2, 1, "Treatment", treatmentField);
        addFormRow(card, 3, 0, "Appointment Date", appointmentDateField);
        addFormRow(card, 3, 1, "Appointment Time", appointmentTimeField);
        addFormRow(card, 4, 0, "Appointment Status", appointmentStatusField);
        return card;
    }

    private JPanel createCalculationCard() {
        JPanel card = createSectionCard("Bill Calculation");
        addFormRow(card, 0, 0, "Treatment Price", treatmentPriceField);
        addFormRow(card, 0, 1, "Consultation Fee", consultationFeeField);
        addFormRow(card, 1, 0, "Subtotal", subtotalField);
        addFormRow(card, 1, 1, "Discount", discountField);
        addFormRow(card, 2, 0, "Tax", taxField);
        addFormRow(card, 2, 1, "Final Total", finalTotalField);
        return card;
    }

    private JPanel createReceiptCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER_COLOR),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JLabel titleLabel = new JLabel("Receipt Preview");
        titleLabel.setFont(SECTION_TITLE_FONT);
        titleLabel.setForeground(DARK_BLUE);

        JScrollPane scrollPane = new JScrollPane(receiptTextArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(FIELD_BORDER_COLOR));
        scrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private JPanel createSectionCard(String title) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER_COLOR),
                new EmptyBorder(14, 17, 14, 17)
        ));

        GridBagConstraints titleConstraints = new GridBagConstraints();
        titleConstraints.gridx = 0;
        titleConstraints.gridy = 0;
        titleConstraints.gridwidth = 4;
        titleConstraints.anchor = GridBagConstraints.WEST;
        titleConstraints.insets = new Insets(0, 0, 10, 0);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SECTION_TITLE_FONT);
        titleLabel.setForeground(DARK_BLUE);
        card.add(titleLabel, titleConstraints);
        return card;
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER_COLOR),
                new EmptyBorder(14, 18, 14, 18)
        ));
        return panel;
    }

    private void addFormRow(
            JPanel panel,
            int row,
            int columnPair,
            String labelText,
            JTextField field
    ) {
        int gridRow = row + 1;
        int labelColumn = columnPair * 2;
        int fieldColumn = labelColumn + 1;

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = labelColumn;
        labelConstraints.gridy = gridRow;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(
                3,
                columnPair == 0 ? 0 : 14,
                3,
                9
        );
        panel.add(createFormLabel(labelText), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = fieldColumn;
        fieldConstraints.gridy = gridRow;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(3, 0, 3, 0);
        panel.add(field, fieldConstraints);
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER_COLOR),
                new EmptyBorder(13, 20, 13, 20)
        ));

        JPanel leftButtons = new JPanel(new GridLayout(1, 2, 10, 0));
        leftButtons.setOpaque(false);
        leftButtons.add(backButton);
        leftButtons.add(clearButton);

        JPanel rightButtons = new JPanel(new GridLayout(1, 2, 10, 0));
        rightButtons.setOpaque(false);
        rightButtons.add(printReceiptButton);
        rightButtons.add(generateBillButton);

        footer.add(leftButtons, BorderLayout.WEST);
        footer.add(rightButtons, BorderLayout.EAST);
        return footer;
    }

    public void displayCalculation(BillingCalculation calculation) {
        appointmentNumberField.setText(calculation.appointmentNumber());
        patientNumberField.setText(safeText(calculation.patientNumber()));
        patientNameField.setText(safeText(calculation.patientName()));
        contactNumberField.setText(safeText(calculation.contactNumber()));
        dentistField.setText(safeText(calculation.dentistName()));
        treatmentField.setText(safeText(calculation.treatmentName()));
        appointmentDateField.setText(
                calculation.appointmentDate().format(DATE_FORMATTER)
        );
        appointmentTimeField.setText(
                calculation.appointmentTime().format(TIME_FORMATTER)
        );
        appointmentStatusField.setText(safeText(calculation.appointmentStatus()));

        treatmentPriceField.setText(formatCurrency(calculation.treatmentPrice()));
        consultationFeeField.setText(formatCurrency(calculation.consultationFee()));
        subtotalField.setText(formatCurrency(calculation.subtotal()));
        discountField.setText(formatCurrency(calculation.discountAmount()));
        taxField.setText(formatCurrency(calculation.taxAmount()));
        finalTotalField.setText(formatCurrency(calculation.totalAmount()));

        calculationLoaded = true;
        billGenerated = false;
        applyControlState();
    }

    public void markBillGenerated() {
        billGenerated = true;
        receiptPrintable = true;
        applyControlState();
    }

    public void markDuplicateBill() {
        billGenerated = true;
        receiptPrintable = false;
        applyControlState();
    }

    public void clearForm() {
        searchField.setText("");
        clearDetails();
        receiptTextArea.setText(emptyReceiptMessage());
        setStatus("Ready", false);
        searchField.requestFocusInWindow();
    }

    public void clearDetails() {
        JTextField[] fields = {
            appointmentNumberField,
            patientNumberField,
            patientNameField,
            contactNumberField,
            dentistField,
            treatmentField,
            appointmentDateField,
            appointmentTimeField,
            appointmentStatusField,
            treatmentPriceField,
            consultationFeeField,
            subtotalField,
            discountField,
            taxField,
            finalTotalField
        };
        for (JTextField field : fields) {
            field.setText("");
        }

        calculationLoaded = false;
        billGenerated = false;
        receiptPrintable = false;
        applyControlState();
    }

    public void setReceiptText(String receiptText) {
        receiptTextArea.setText(receiptText == null ? "" : receiptText);
        receiptTextArea.setCaretPosition(0);
    }

    public void setBusy(boolean busy, String status) {
        this.busy = busy;
        setCursor(busy
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor());
        setStatus(status, false);
        applyControlState();
    }

    public void setStatus(String message, boolean error) {
        statusLabel.setText(message == null || message.isBlank() ? " " : message);
        statusLabel.setForeground(error ? ERROR_COLOR : MUTED_TEXT_COLOR);
    }

    public String getSearchAppointmentNumber() {
        return searchField.getText();
    }

    public void addSearchListener(ActionListener listener) {
        searchButton.addActionListener(listener);
        searchField.addActionListener(listener);
    }

    public void addGenerateBillListener(ActionListener listener) {
        generateBillButton.addActionListener(listener);
    }

    public void addClearListener(ActionListener listener) {
        clearButton.addActionListener(listener);
    }

    public void addBackListener(ActionListener listener) {
        backButton.addActionListener(listener);
    }

    public void addPrintReceiptListener(ActionListener listener) {
        printReceiptButton.addActionListener(listener);
    }

    /**
     * Opens the standard print dialog for the final receipt text.
     *
     * @return {@code true} if printing was accepted; {@code false} if cancelled
     * @throws PrinterException if the print system cannot complete the request
     */
    public boolean printReceipt() throws PrinterException {
        MessageFormat header = new MessageFormat(
                "Sunrise Dental Clinic - Patient Receipt"
        );
        MessageFormat footer = new MessageFormat("Page {0}");
        return receiptTextArea.print(header, footer);
    }

    public void showSuccessMessage(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Bill Generated",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public void showInformationMessage(String title, String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public void showErrorMessage(String title, String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void applyControlState() {
        searchField.setEnabled(!busy);
        searchButton.setEnabled(!busy);
        generateBillButton.setEnabled(
                !busy && calculationLoaded && !billGenerated
        );
        clearButton.setEnabled(!busy);
        backButton.setEnabled(!busy);

        printReceiptButton.setEnabled(!busy && receiptPrintable);
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return String.format(Locale.ROOT, "LKR %,.2f", value);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String emptyReceiptMessage() {
        return "\n\n"
                + "                 RECEIPT PREVIEW\n"
                + "\n"
                + "       Search for an appointment to calculate\n"
                + "       and preview its patient bill.";
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_COLOR);
        return label;
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(INPUT_FONT);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(PRIMARY_BLUE);
        field.setPreferredSize(new Dimension(300, 34));
        field.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER_COLOR),
                new EmptyBorder(6, 9, 6, 9)
        ));
        return field;
    }

    private JTextField createReadOnlyField() {
        JTextField field = createTextField();
        field.setEditable(false);
        field.setBackground(READ_ONLY_BACKGROUND);
        return field;
    }

    private JTextField createMoneyField() {
        JTextField field = createReadOnlyField();
        field.setHorizontalAlignment(JTextField.RIGHT);
        return field;
    }

    private JButton createPrimaryButton(String text, int width) {
        JButton button = createBaseButton(text, width);
        button.setBackground(PRIMARY_BLUE);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        return button;
    }

    private JButton createSecondaryButton(String text, int width) {
        JButton button = createBaseButton(text, width);
        button.setBackground(Color.WHITE);
        button.setForeground(DARK_BLUE);
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_BLUE),
                new EmptyBorder(9, 14, 9, 14)
        ));
        return button;
    }

    private JButton createBaseButton(String text, int width) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(width, 40));
        return button;
    }

    private BufferedImage createApplicationIcon() {
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            graphics.setColor(PRIMARY_BLUE);
            graphics.fillRoundRect(1, 1, 30, 30, 9, 9);
            graphics.setColor(Color.WHITE);
            graphics.setStroke(new BasicStroke(
                    4.0f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            ));
            graphics.drawLine(16, 8, 16, 24);
            graphics.drawLine(8, 16, 24, 16);
        } finally {
            graphics.dispose();
        }
        return icon;
    }
}
