package com.mycompany.sunrisedentalclinicmanagementsystem.ui;

import com.mycompany.sunrisedentalclinicmanagementsystem.model.Dentist;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Treatment;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 * Presents the patient and appointment registration form.
 */
public final class RegisterAppointmentFrame extends JFrame {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Colombo");

    private static final Color PRIMARY_BLUE = new Color(22, 91, 170);
    private static final Color DARK_BLUE = new Color(14, 55, 102);
    private static final Color TEXT_COLOR = new Color(37, 50, 66);
    private static final Color MUTED_TEXT_COLOR = new Color(105, 118, 132);
    private static final Color FIELD_BORDER_COLOR = new Color(202, 213, 224);
    private static final Color CARD_BORDER_COLOR = new Color(218, 226, 234);
    private static final Color WINDOW_BACKGROUND = new Color(242, 247, 252);
    private static final Color ERROR_COLOR = new Color(181, 48, 48);

    private static final Font HEADER_TITLE_FONT
            = new Font("SansSerif", Font.BOLD, 23);
    private static final Font HEADER_SUBTITLE_FONT
            = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font SECTION_TITLE_FONT
            = new Font("SansSerif", Font.BOLD, 18);
    private static final Font LABEL_FONT
            = new Font("SansSerif", Font.BOLD, 13);
    private static final Font INPUT_FONT
            = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font BUTTON_FONT
            = new Font("SansSerif", Font.BOLD, 14);

    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField addressLine1Field;
    private JTextField addressLine2Field;
    private JTextField cityField;
    private JTextField contactNumberField;
    private JTextField appointmentNumberField;
    private JComboBox<Dentist> dentistComboBox;
    private JComboBox<Treatment> treatmentComboBox;
    private JTextField treatmentPriceField;
    private JSpinner appointmentDateSpinner;
    private JSpinner appointmentTimeSpinner;
    private JTextArea notesTextArea;
    private JButton registerButton;
    private JButton clearButton;
    private JButton cancelButton;
    private JLabel statusLabel;

    public RegisterAppointmentFrame() {
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        setTitle("Sunrise Dental Clinic Management System - Register Appointment");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setIconImage(createApplicationIcon());

        firstNameField = createTextField();
        lastNameField = createTextField();
        addressLine1Field = createTextField();
        addressLine2Field = createTextField();
        cityField = createTextField();
        contactNumberField = createTextField();
        appointmentNumberField = createTextField();

        dentistComboBox = createComboBox();
        treatmentComboBox = createComboBox();

        treatmentPriceField = createTextField();
        treatmentPriceField.setEditable(false);
        treatmentPriceField.setBackground(new Color(247, 249, 251));
        treatmentPriceField.setText("Select a treatment");

        appointmentDateSpinner = createDateSpinner();
        appointmentTimeSpinner = createTimeSpinner();

        notesTextArea = new JTextArea(3, 24);
        notesTextArea.setFont(INPUT_FONT);
        notesTextArea.setForeground(TEXT_COLOR);
        notesTextArea.setLineWrap(true);
        notesTextArea.setWrapStyleWord(true);
        notesTextArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        registerButton = createPrimaryButton("Register Appointment");
        registerButton.setPreferredSize(new Dimension(220, 42));
        clearButton = createSecondaryButton("Clear Form");
        cancelButton = createSecondaryButton("Cancel / Back");

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusLabel.setForeground(MUTED_TEXT_COLOR);

        getRootPane().setDefaultButton(registerButton);
    }

    private void setupLayout() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setPreferredSize(new Dimension(1100, 750));
        rootPanel.setBackground(WINDOW_BACKGROUND);
        rootPanel.add(createHeader(), BorderLayout.NORTH);
        rootPanel.add(createFormArea(), BorderLayout.CENTER);
        rootPanel.add(createFooter(), BorderLayout.SOUTH);

        setContentPane(rootPanel);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new GridBagLayout());
        headerPanel.setBackground(DARK_BLUE);
        headerPanel.setBorder(new EmptyBorder(22, 34, 22, 34));
        headerPanel.setPreferredSize(new Dimension(0, 118));

        JLabel clinicLabel = new JLabel(
                "Sunrise Dental Clinic Management System"
        );
        clinicLabel.setFont(HEADER_TITLE_FONT);
        clinicLabel.setForeground(Color.WHITE);

        JLabel pageTitleLabel = new JLabel("Register New Appointment");
        pageTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        pageTitleLabel.setForeground(new Color(218, 234, 250));

        JLabel subtitleLabel = new JLabel(
                "Create a patient record and schedule a dental appointment"
        );
        subtitleLabel.setFont(HEADER_SUBTITLE_FONT);
        subtitleLabel.setForeground(new Color(183, 211, 238));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.gridy = 0;
        headerPanel.add(clinicLabel, constraints);
        constraints.gridy = 1;
        constraints.insets = new Insets(7, 0, 0, 0);
        headerPanel.add(pageTitleLabel, constraints);
        constraints.gridy = 2;
        constraints.insets = new Insets(5, 0, 0, 0);
        headerPanel.add(subtitleLabel, constraints);
        return headerPanel;
    }

    private JPanel createFormArea() {
        JPanel formArea = new JPanel(new GridLayout(1, 2, 22, 0));
        formArea.setBackground(WINDOW_BACKGROUND);
        formArea.setBorder(new EmptyBorder(24, 28, 20, 28));
        formArea.add(createPatientDetailsCard());
        formArea.add(createAppointmentDetailsCard());
        return formArea;
    }

    private JPanel createPatientDetailsCard() {
        JPanel card = createSectionCard("Patient Details");
        addFormRow(card, 1, "First Name *", firstNameField);
        addFormRow(card, 2, "Last Name *", lastNameField);
        addFormRow(card, 3, "Address Line 1 *", addressLine1Field);
        addFormRow(card, 4, "Address Line 2", addressLine2Field);
        addFormRow(card, 5, "City", cityField);
        addFormRow(card, 6, "Contact Number *", contactNumberField);
        addVerticalFiller(card, 7);
        return card;
    }

    private JPanel createAppointmentDetailsCard() {
        JPanel card = createSectionCard("Appointment Details");
        addFormRow(card, 1, "Appointment Number *", appointmentNumberField);
        addFormRow(card, 2, "Dentist *", dentistComboBox);
        addFormRow(card, 3, "Treatment *", treatmentComboBox);
        addFormRow(card, 4, "Treatment Price", treatmentPriceField);
        addFormRow(card, 5, "Appointment Date *", appointmentDateSpinner);
        addFormRow(card, 6, "Appointment Time *", appointmentTimeSpinner);

        JScrollPane notesScrollPane = new JScrollPane(notesTextArea);
        notesScrollPane.setBorder(BorderFactory.createLineBorder(
                FIELD_BORDER_COLOR,
                1
        ));
        notesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        notesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        notesScrollPane.setPreferredSize(new Dimension(280, 82));
        addFormRow(card, 7, "Notes", notesScrollPane);
        return card;
    }

    private JPanel createSectionCard(String title) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER_COLOR),
                new EmptyBorder(22, 24, 24, 24)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SECTION_TITLE_FONT);
        titleLabel.setForeground(DARK_BLUE);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 18, 0);
        card.add(titleLabel, constraints);
        return card;
    }

    private void addFormRow(
            JPanel panel,
            int row,
            String labelText,
            JComponent component
    ) {
        JLabel label = new JLabel(labelText);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_COLOR);
        label.setPreferredSize(new Dimension(145, 22));

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(0, 0, 14, 14);
        panel.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.anchor = GridBagConstraints.NORTH;
        fieldConstraints.insets = new Insets(0, 0, 14, 0);
        panel.add(component, fieldConstraints);
    }

    private void addVerticalFiller(JPanel panel, int row) {
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.VERTICAL;
        panel.add(filler, constraints);
    }

    private JPanel createFooter() {
        JPanel footerPanel = new JPanel(new BorderLayout(20, 0));
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER_COLOR),
                new EmptyBorder(15, 28, 15, 28)
        ));
        footerPanel.setPreferredSize(new Dimension(0, 76));

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(registerButton);

        footerPanel.add(statusLabel, BorderLayout.CENTER);
        footerPanel.add(buttonPanel, BorderLayout.EAST);
        return footerPanel;
    }

    public void setDentists(List<Dentist> dentists) {
        dentistComboBox.setModel(new DefaultComboBoxModel<>(
                dentists.toArray(Dentist[]::new)
        ));
        dentistComboBox.setSelectedIndex(-1);
    }

    public void setTreatments(List<Treatment> treatments) {
        treatmentComboBox.setModel(new DefaultComboBoxModel<>(
                treatments.toArray(Treatment[]::new)
        ));
        treatmentComboBox.setSelectedIndex(-1);
        setTreatmentPrice(null);
    }

    public void setTreatmentPrice(BigDecimal price) {
        if (price == null) {
            treatmentPriceField.setText("Select a treatment");
            return;
        }
        treatmentPriceField.setText(String.format(
                Locale.ROOT,
                "LKR %,.2f",
                price
        ));
    }

    public void setBusy(boolean busy, String status) {
        boolean enabled = !busy;
        for (Component component : formComponents()) {
            component.setEnabled(enabled);
        }
        registerButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        setStatus(status, false);
        setCursor(busy
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor());
    }

    public void setStatus(String message, boolean error) {
        statusLabel.setText(message == null || message.isBlank() ? " " : message);
        statusLabel.setForeground(error ? ERROR_COLOR : MUTED_TEXT_COLOR);
    }

    public void clearForm() {
        firstNameField.setText("");
        lastNameField.setText("");
        addressLine1Field.setText("");
        addressLine2Field.setText("");
        cityField.setText("");
        contactNumberField.setText("");
        appointmentNumberField.setText("");
        dentistComboBox.setSelectedIndex(-1);
        treatmentComboBox.setSelectedIndex(-1);
        setTreatmentPrice(null);
        appointmentDateSpinner.setValue(currentClinicDate());
        appointmentTimeSpinner.setValue(defaultAppointmentTime());
        notesTextArea.setText("");
        firstNameField.requestFocusInWindow();
    }

    public void showSuccessMessage(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Appointment Registered",
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

    public void showWarningMessage(String title, String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.WARNING_MESSAGE
        );
    }

    public void addRegisterListener(ActionListener listener) {
        registerButton.addActionListener(listener);
    }

    public void addClearListener(ActionListener listener) {
        clearButton.addActionListener(listener);
    }

    public void addCancelListener(ActionListener listener) {
        cancelButton.addActionListener(listener);
    }

    public void addTreatmentSelectionListener(ActionListener listener) {
        treatmentComboBox.addActionListener(listener);
    }

    public String getPatientFirstName() {
        return firstNameField.getText();
    }

    public String getPatientLastName() {
        return lastNameField.getText();
    }

    public String getAddressLine1() {
        return addressLine1Field.getText();
    }

    public String getAddressLine2() {
        return addressLine2Field.getText();
    }

    public String getCity() {
        return cityField.getText();
    }

    public String getContactNumber() {
        return contactNumberField.getText();
    }

    public String getAppointmentNumber() {
        return appointmentNumberField.getText();
    }

    public Dentist getSelectedDentist() {
        return (Dentist) dentistComboBox.getSelectedItem();
    }

    public Treatment getSelectedTreatment() {
        return (Treatment) treatmentComboBox.getSelectedItem();
    }

    public LocalDate getAppointmentDate() {
        Date selectedDate = (Date) appointmentDateSpinner.getValue();
        return selectedDate.toInstant().atZone(CLINIC_ZONE).toLocalDate();
    }

    public LocalTime getAppointmentTime() {
        Date selectedTime = (Date) appointmentTimeSpinner.getValue();
        return selectedTime.toInstant()
                .atZone(CLINIC_ZONE)
                .toLocalTime()
                .withSecond(0)
                .withNano(0);
    }

    public String getNotes() {
        return notesTextArea.getText();
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(INPUT_FONT);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(PRIMARY_BLUE);
        field.setPreferredSize(new Dimension(280, 40));
        field.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER_COLOR),
                new EmptyBorder(7, 10, 7, 10)
        ));
        return field;
    }

    private <T> JComboBox<T> createComboBox() {
        JComboBox<T> comboBox = new JComboBox<>();
        comboBox.setFont(INPUT_FONT);
        comboBox.setForeground(TEXT_COLOR);
        comboBox.setBackground(Color.WHITE);
        comboBox.setPreferredSize(new Dimension(280, 40));
        return comboBox;
    }

    private JSpinner createDateSpinner() {
        Date currentDate = currentClinicDate();
        JSpinner spinner = new JSpinner(new SpinnerDateModel(
                currentDate,
                null,
                null,
                Calendar.DAY_OF_MONTH
        ));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        styleSpinner(spinner);
        return spinner;
    }

    private Date currentClinicDate() {
        return Date.from(LocalDate.now(CLINIC_ZONE)
                .atStartOfDay(CLINIC_ZONE)
                .toInstant());
    }

    private JSpinner createTimeSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        spinner.setValue(defaultAppointmentTime());
        spinner.setEditor(new JSpinner.DateEditor(spinner, "hh:mm a"));
        styleSpinner(spinner);
        return spinner;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(INPUT_FONT);
        spinner.setPreferredSize(new Dimension(280, 40));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setFont(INPUT_FONT);
        editor.getTextField().setForeground(TEXT_COLOR);
        editor.getTextField().setBorder(new EmptyBorder(7, 10, 7, 10));
        spinner.setBorder(BorderFactory.createLineBorder(FIELD_BORDER_COLOR));
    }

    private JButton createPrimaryButton(String text) {
        JButton button = createBaseButton(text);
        button.setBackground(PRIMARY_BLUE);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(11, 18, 11, 18));
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = createBaseButton(text);
        button.setBackground(Color.WHITE);
        button.setForeground(DARK_BLUE);
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_BLUE),
                new EmptyBorder(10, 17, 10, 17)
        ));
        return button;
    }

    private JButton createBaseButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(170, 42));
        return button;
    }

    private Component[] formComponents() {
        return new Component[]{
            firstNameField,
            lastNameField,
            addressLine1Field,
            addressLine2Field,
            cityField,
            contactNumberField,
            appointmentNumberField,
            dentistComboBox,
            treatmentComboBox,
            appointmentDateSpinner,
            appointmentTimeSpinner,
            notesTextArea
        };
    }

    private Date defaultAppointmentTime() {
        ZonedDateTime defaultTime = ZonedDateTime.now(CLINIC_ZONE)
                .plusMinutes(30)
                .withSecond(0)
                .withNano(0);
        return Date.from(defaultTime.toInstant());
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
