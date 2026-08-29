package com.mycompany.sunrisedentalclinicmanagementsystem.ui;

import com.mycompany.sunrisedentalclinicmanagementsystem.model.AppointmentDetails;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Dentist;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Treatment;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
 * Presents appointment-number search and maintenance controls.
 */
public final class SearchAppointmentFrame extends JFrame {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Colombo");

    private static final Color PRIMARY_BLUE = new Color(22, 91, 170);
    private static final Color DARK_BLUE = new Color(14, 55, 102);
    private static final Color TEXT_COLOR = new Color(37, 50, 66);
    private static final Color MUTED_TEXT_COLOR = new Color(105, 118, 132);
    private static final Color FIELD_BORDER_COLOR = new Color(202, 213, 224);
    private static final Color CARD_BORDER_COLOR = new Color(218, 226, 234);
    private static final Color WINDOW_BACKGROUND = new Color(242, 247, 252);
    private static final Color READ_ONLY_BACKGROUND = new Color(247, 249, 251);
    private static final Color ERROR_COLOR = new Color(181, 53, 53);
    private static final Color SUCCESS_COLOR = new Color(32, 128, 92);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font PAGE_TITLE_FONT = new Font("SansSerif", Font.BOLD, 19);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font SECTION_FONT = new Font("SansSerif", Font.BOLD, 17);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font INPUT_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 13);

    private JTextField searchField;
    private JButton searchButton;
    private JLabel statusLabel;

    private JTextField patientNumberField;
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
    private JTextField appointmentStatusField;
    private JTextArea notesTextArea;
    private JTextField createdByField;

    private JButton editButton;
    private JButton saveButton;
    private JButton cancelEditButton;
    private JButton cancelAppointmentButton;
    private JButton clearButton;
    private JButton backButton;

    private boolean appointmentLoaded;
    private boolean editMode;
    private boolean cancelledAppointment;
    private boolean busy;
    private boolean referenceDataAvailable;

    public SearchAppointmentFrame() {
        initializeComponents();
        setupLayout();
        clearForm();
    }

    private void initializeComponents() {
        setTitle("Sunrise Dental Clinic Management System - Search Appointment");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setIconImage(createApplicationIcon());

        searchField = createTextField();
        searchField.setPreferredSize(new Dimension(360, 40));
        searchButton = createPrimaryButton("Search");
        searchButton.setPreferredSize(new Dimension(125, 40));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(SUBTITLE_FONT);
        statusLabel.setForeground(MUTED_TEXT_COLOR);

        patientNumberField = createReadOnlyField();
        firstNameField = createTextField();
        lastNameField = createTextField();
        addressLine1Field = createTextField();
        addressLine2Field = createTextField();
        cityField = createTextField();
        contactNumberField = createTextField();

        appointmentNumberField = createReadOnlyField();
        dentistComboBox = createComboBox();
        treatmentComboBox = createComboBox();
        treatmentPriceField = createReadOnlyField();
        appointmentDateSpinner = createDateSpinner();
        appointmentTimeSpinner = createTimeSpinner();
        appointmentStatusField = createReadOnlyField();
        notesTextArea = createNotesArea();
        createdByField = createReadOnlyField();

        editButton = createPrimaryButton("Edit");
        saveButton = createPrimaryButton("Save Changes");
        saveButton.setPreferredSize(new Dimension(150, 42));
        cancelEditButton = createSecondaryButton("Cancel Edit");
        cancelEditButton.setPreferredSize(new Dimension(135, 42));
        cancelAppointmentButton = createDangerButton("Cancel Appointment");
        cancelAppointmentButton.setPreferredSize(new Dimension(185, 42));
        clearButton = createSecondaryButton("Clear");
        backButton = createSecondaryButton("Back to Dashboard");
        backButton.setPreferredSize(new Dimension(170, 42));

    }

    private void setupLayout() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setPreferredSize(new Dimension(1180, 850));
        rootPanel.setBackground(WINDOW_BACKGROUND);
        rootPanel.add(createHeader(), BorderLayout.NORTH);
        rootPanel.add(createContent(), BorderLayout.CENTER);
        rootPanel.add(createFooter(), BorderLayout.SOUTH);

        setContentPane(rootPanel);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBackground(DARK_BLUE);
        headerPanel.setBorder(new EmptyBorder(18, 34, 18, 34));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 1.0;

        JLabel clinicLabel = new JLabel(
                "Sunrise Dental Clinic Management System"
        );
        clinicLabel.setFont(TITLE_FONT);
        clinicLabel.setForeground(Color.WHITE);
        constraints.gridy = 0;
        headerPanel.add(clinicLabel, constraints);

        JLabel pageTitleLabel = new JLabel("Search & Manage Appointment");
        pageTitleLabel.setFont(PAGE_TITLE_FONT);
        pageTitleLabel.setForeground(new Color(218, 234, 250));
        constraints.gridy = 1;
        constraints.insets = new Insets(5, 0, 0, 0);
        headerPanel.add(pageTitleLabel, constraints);

        JLabel subtitleLabel = new JLabel(
                "Search, review and maintain existing appointment records."
        );
        subtitleLabel.setFont(SUBTITLE_FONT);
        subtitleLabel.setForeground(new Color(190, 216, 241));
        constraints.gridy = 2;
        constraints.insets = new Insets(4, 0, 0, 0);
        headerPanel.add(subtitleLabel, constraints);
        return headerPanel;
    }

    private JPanel createContent() {
        JPanel contentPanel = new JPanel(new BorderLayout(0, 16));
        contentPanel.setBackground(WINDOW_BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(18, 28, 16, 28));
        contentPanel.add(createSearchPanel(), BorderLayout.NORTH);

        JPanel detailsPanel = new JPanel(new GridLayout(1, 2, 18, 0));
        detailsPanel.setOpaque(false);
        detailsPanel.add(createPatientDetailsCard());
        detailsPanel.add(createAppointmentDetailsCard());
        contentPanel.add(detailsPanel, BorderLayout.CENTER);
        return contentPanel;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER_COLOR),
                new EmptyBorder(14, 20, 14, 20)
        ));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;

        JLabel label = createFormLabel("Appointment Number");
        constraints.gridx = 0;
        constraints.insets = new Insets(0, 0, 0, 12);
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 12);
        panel.add(searchField, constraints);

        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        panel.add(searchButton, constraints);

        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(7, 0, 0, 0);
        panel.add(statusLabel, constraints);
        return panel;
    }

    private JPanel createPatientDetailsCard() {
        JPanel card = createSectionCard("Patient Details");
        addFormRow(card, 1, "Patient Number", patientNumberField);
        addFormRow(card, 2, "First Name *", firstNameField);
        addFormRow(card, 3, "Last Name *", lastNameField);
        addFormRow(card, 4, "Address Line 1 *", addressLine1Field);
        addFormRow(card, 5, "Address Line 2", addressLine2Field);
        addFormRow(card, 6, "City", cityField);
        addFormRow(card, 7, "Contact Number *", contactNumberField);
        return card;
    }

    private JPanel createAppointmentDetailsCard() {
        JPanel card = createSectionCard("Appointment Details");
        addFormRow(card, 1, "Appointment Number", appointmentNumberField);
        addFormRow(card, 2, "Dentist *", dentistComboBox);
        addFormRow(card, 3, "Treatment *", treatmentComboBox);
        addFormRow(card, 4, "Treatment Price", treatmentPriceField);
        addFormRow(card, 5, "Appointment Date *", appointmentDateSpinner);
        addFormRow(card, 6, "Appointment Time *", appointmentTimeSpinner);
        addFormRow(card, 7, "Status", appointmentStatusField);

        JScrollPane notesScrollPane = new JScrollPane(notesTextArea);
        notesScrollPane.setBorder(BorderFactory.createLineBorder(
                FIELD_BORDER_COLOR
        ));
        notesScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        notesScrollPane.setPreferredSize(new Dimension(300, 76));
        addFormRow(card, 8, "Notes", notesScrollPane);
        addFormRow(card, 9, "Created By", createdByField);
        return card;
    }

    private JPanel createSectionCard(String title) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER_COLOR),
                new EmptyBorder(18, 20, 18, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SECTION_FONT);
        titleLabel.setForeground(DARK_BLUE);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 12, 0);
        card.add(titleLabel, constraints);
        return card;
    }

    private void addFormRow(
            JPanel panel,
            int row,
            String labelText,
            Component component
    ) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(7, 0, 7, 14);
        panel.add(createFormLabel(labelText), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.anchor = GridBagConstraints.NORTHWEST;
        fieldConstraints.insets = new Insets(3, 0, 3, 0);
        panel.add(component, fieldConstraints);
    }

    private JPanel createFooter() {
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER_COLOR),
                new EmptyBorder(14, 24, 14, 24)
        ));

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftButtons.setOpaque(false);
        leftButtons.add(backButton);
        leftButtons.add(clearButton);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightButtons.setOpaque(false);
        rightButtons.add(cancelAppointmentButton);
        rightButtons.add(cancelEditButton);
        rightButtons.add(editButton);
        rightButtons.add(saveButton);

        footerPanel.add(leftButtons, BorderLayout.WEST);
        footerPanel.add(rightButtons, BorderLayout.EAST);
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
    }

    public void setReferenceDataAvailable(boolean available) {
        referenceDataAvailable = available;
        applyControlState();
    }

    public void displayAppointmentDetails(AppointmentDetails details) {
        patientNumberField.setText(safeText(details.patientNumber()));
        firstNameField.setText(safeText(details.firstName()));
        lastNameField.setText(safeText(details.lastName()));
        addressLine1Field.setText(safeText(details.addressLine1()));
        addressLine2Field.setText(safeText(details.addressLine2()));
        cityField.setText(safeText(details.city()));
        contactNumberField.setText(safeText(details.contactNumber()));

        appointmentNumberField.setText(safeText(details.appointmentNumber()));
        selectDentist(details);
        selectTreatment(details);
        setTreatmentPrice(details.treatmentPrice());
        appointmentDateSpinner.setValue(toDate(details.appointmentDate()));
        appointmentTimeSpinner.setValue(toDate(details.appointmentTime()));
        appointmentStatusField.setText(safeText(details.status()));
        notesTextArea.setText(safeText(details.notes()));
        createdByField.setText(formatCreatedBy(details));

        appointmentLoaded = true;
        editMode = false;
        cancelledAppointment = "CANCELLED".equals(details.status());
        updateStatusFieldStyle();
        applyControlState();
    }

    public void clearForm() {
        searchField.setText("");
        clearAppointmentDetails();
        setStatus("Ready", false);
        searchField.requestFocusInWindow();
    }

    public void clearAppointmentDetails() {
        patientNumberField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        addressLine1Field.setText("");
        addressLine2Field.setText("");
        cityField.setText("");
        contactNumberField.setText("");
        appointmentNumberField.setText("");
        dentistComboBox.setSelectedIndex(-1);
        treatmentComboBox.setSelectedIndex(-1);
        treatmentPriceField.setText("");
        appointmentDateSpinner.setValue(currentClinicDate());
        appointmentTimeSpinner.setValue(currentClinicTime());
        appointmentStatusField.setText("");
        notesTextArea.setText("");
        createdByField.setText("");

        appointmentLoaded = false;
        editMode = false;
        cancelledAppointment = false;
        updateStatusFieldStyle();
        applyControlState();
    }

    public void setEditMode(boolean editing) {
        editMode = editing && appointmentLoaded && !cancelledAppointment;
        applyControlState();
    }

    public void setBusy(boolean busy, String status) {
        this.busy = busy;
        setStatus(status, false);
        setCursor(busy
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor());
        applyControlState();
    }

    public void setStatus(String message, boolean error) {
        statusLabel.setText(message == null || message.isBlank() ? " " : message);
        statusLabel.setForeground(error ? ERROR_COLOR : MUTED_TEXT_COLOR);
    }

    public boolean confirmCancellation() {
        return JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel this appointment?",
                "Confirm Appointment Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        ) == JOptionPane.YES_OPTION;
    }

    public void showSuccessMessage(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Appointment Management",
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

    public void showWarningMessage(String title, String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.WARNING_MESSAGE
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

    public void addSearchListener(ActionListener listener) {
        searchButton.addActionListener(listener);
        searchField.addActionListener(listener);
    }

    public void addEditListener(ActionListener listener) {
        editButton.addActionListener(listener);
    }

    public void addSaveListener(ActionListener listener) {
        saveButton.addActionListener(listener);
    }

    public void addCancelEditListener(ActionListener listener) {
        cancelEditButton.addActionListener(listener);
    }

    public void addCancelAppointmentListener(ActionListener listener) {
        cancelAppointmentButton.addActionListener(listener);
    }

    public void addClearListener(ActionListener listener) {
        clearButton.addActionListener(listener);
    }

    public void addBackListener(ActionListener listener) {
        backButton.addActionListener(listener);
    }

    public void addTreatmentSelectionListener(ActionListener listener) {
        treatmentComboBox.addActionListener(listener);
    }

    public String getSearchAppointmentNumber() {
        return searchField.getText();
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

    public Dentist getSelectedDentist() {
        return (Dentist) dentistComboBox.getSelectedItem();
    }

    public Treatment getSelectedTreatment() {
        return (Treatment) treatmentComboBox.getSelectedItem();
    }

    public LocalDate getAppointmentDate() {
        return ((Date) appointmentDateSpinner.getValue()).toInstant()
                .atZone(CLINIC_ZONE)
                .toLocalDate();
    }

    public LocalTime getAppointmentTime() {
        return ((Date) appointmentTimeSpinner.getValue()).toInstant()
                .atZone(CLINIC_ZONE)
                .toLocalTime()
                .withSecond(0)
                .withNano(0);
    }

    public String getNotes() {
        return notesTextArea.getText();
    }

    public void setTreatmentPrice(BigDecimal price) {
        if (price == null) {
            treatmentPriceField.setText("");
            return;
        }
        treatmentPriceField.setText(String.format(
                Locale.ROOT,
                "LKR %,.2f",
                price
        ));
    }

    private void applyControlState() {
        boolean editable = appointmentLoaded
                && editMode
                && !cancelledAppointment
                && !busy;
        setEditable(firstNameField, editable);
        setEditable(lastNameField, editable);
        setEditable(addressLine1Field, editable);
        setEditable(addressLine2Field, editable);
        setEditable(cityField, editable);
        setEditable(contactNumberField, editable);
        notesTextArea.setEditable(editable);
        notesTextArea.setBackground(editable ? Color.WHITE : READ_ONLY_BACKGROUND);
        dentistComboBox.setEnabled(editable);
        treatmentComboBox.setEnabled(editable);
        appointmentDateSpinner.setEnabled(editable);
        appointmentTimeSpinner.setEnabled(editable);

        searchField.setEnabled(!busy);
        searchButton.setEnabled(!busy);
        editButton.setEnabled(!busy
                && appointmentLoaded
                && !editMode
                && !cancelledAppointment
                && referenceDataAvailable);
        saveButton.setEnabled(editable);
        cancelEditButton.setEnabled(!busy && appointmentLoaded && editMode);
        cancelAppointmentButton.setEnabled(!busy
                && appointmentLoaded
                && !editMode
                && !cancelledAppointment);
        clearButton.setEnabled(!busy);
        backButton.setEnabled(!busy);
    }

    private void selectDentist(AppointmentDetails details) {
        if (selectDentistById(details.dentistId())) {
            return;
        }

        Dentist historicalDentist = new Dentist(
                details.dentistId(),
                details.dentistRegistrationNumber(),
                details.dentistName(),
                details.dentistSpecialization(),
                false
        );
        dentistComboBox.addItem(historicalDentist);
        dentistComboBox.setSelectedItem(historicalDentist);
    }

    private boolean selectDentistById(long dentistId) {
        for (int index = 0; index < dentistComboBox.getItemCount(); index++) {
            Dentist dentist = dentistComboBox.getItemAt(index);
            if (dentist.getDentistId() == dentistId) {
                dentistComboBox.setSelectedIndex(index);
                return true;
            }
        }
        return false;
    }

    private void selectTreatment(AppointmentDetails details) {
        if (selectTreatmentById(details.treatmentId())) {
            return;
        }

        Treatment historicalTreatment = new Treatment(
                details.treatmentId(),
                details.treatmentCode(),
                details.treatmentName(),
                details.treatmentDescription(),
                details.treatmentPrice(),
                false
        );
        treatmentComboBox.addItem(historicalTreatment);
        treatmentComboBox.setSelectedItem(historicalTreatment);
    }

    private boolean selectTreatmentById(long treatmentId) {
        for (int index = 0; index < treatmentComboBox.getItemCount(); index++) {
            Treatment treatment = treatmentComboBox.getItemAt(index);
            if (treatment.getTreatmentId() == treatmentId) {
                treatmentComboBox.setSelectedIndex(index);
                return true;
            }
        }
        return false;
    }

    private void updateStatusFieldStyle() {
        if (cancelledAppointment) {
            appointmentStatusField.setForeground(ERROR_COLOR);
            appointmentStatusField.setFont(new Font("SansSerif", Font.BOLD, 14));
            return;
        }
        if (appointmentLoaded) {
            appointmentStatusField.setForeground(SUCCESS_COLOR);
            appointmentStatusField.setFont(new Font("SansSerif", Font.BOLD, 14));
            return;
        }
        appointmentStatusField.setForeground(TEXT_COLOR);
        appointmentStatusField.setFont(INPUT_FONT);
    }

    private String formatCreatedBy(AppointmentDetails details) {
        String fullName = safeText(details.createdByFullName());
        String username = safeText(details.createdByUsername());
        if (fullName.isBlank()) {
            return username;
        }
        return username.isBlank() ? fullName : fullName + " (" + username + ")";
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();
        styleTextField(field);
        return field;
    }

    private JTextField createReadOnlyField() {
        JTextField field = createTextField();
        field.setEditable(false);
        field.setBackground(READ_ONLY_BACKGROUND);
        return field;
    }

    private void styleTextField(JTextField field) {
        field.setFont(INPUT_FONT);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(PRIMARY_BLUE);
        field.setPreferredSize(new Dimension(300, 38));
        field.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER_COLOR),
                new EmptyBorder(7, 10, 7, 10)
        ));
    }

    private void setEditable(JTextField field, boolean editable) {
        field.setEditable(editable);
        field.setBackground(editable ? Color.WHITE : READ_ONLY_BACKGROUND);
    }

    private <T> JComboBox<T> createComboBox() {
        JComboBox<T> comboBox = new JComboBox<>();
        comboBox.setFont(INPUT_FONT);
        comboBox.setForeground(TEXT_COLOR);
        comboBox.setBackground(Color.WHITE);
        comboBox.setPreferredSize(new Dimension(300, 38));
        return comboBox;
    }

    private JTextArea createNotesArea() {
        JTextArea textArea = new JTextArea(3, 24);
        textArea.setFont(INPUT_FONT);
        textArea.setForeground(TEXT_COLOR);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(new EmptyBorder(7, 10, 7, 10));
        return textArea;
    }

    private JSpinner createDateSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerDateModel(
                currentClinicDate(),
                null,
                null,
                Calendar.DAY_OF_MONTH
        ));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        styleSpinner(spinner);
        return spinner;
    }

    private JSpinner createTimeSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        spinner.setValue(currentClinicTime());
        spinner.setEditor(new JSpinner.DateEditor(spinner, "hh:mm a"));
        styleSpinner(spinner);
        return spinner;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(INPUT_FONT);
        spinner.setPreferredSize(new Dimension(300, 38));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setFont(INPUT_FONT);
        editor.getTextField().setForeground(TEXT_COLOR);
        editor.getTextField().setBorder(new EmptyBorder(7, 10, 7, 10));
        spinner.setBorder(BorderFactory.createLineBorder(FIELD_BORDER_COLOR));
    }

    private Date currentClinicDate() {
        return toDate(LocalDate.now(CLINIC_ZONE));
    }

    private Date currentClinicTime() {
        return Date.from(LocalDate.now(CLINIC_ZONE)
                .atTime(LocalTime.now(CLINIC_ZONE).withSecond(0).withNano(0))
                .atZone(CLINIC_ZONE)
                .toInstant());
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(CLINIC_ZONE).toInstant());
    }

    private Date toDate(LocalTime time) {
        return Date.from(LocalDate.now(CLINIC_ZONE)
                .atTime(time)
                .atZone(CLINIC_ZONE)
                .toInstant());
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_COLOR);
        return label;
    }

    private JButton createPrimaryButton(String text) {
        JButton button = createBaseButton(text);
        button.setBackground(PRIMARY_BLUE);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 17, 10, 17));
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = createBaseButton(text);
        button.setBackground(Color.WHITE);
        button.setForeground(DARK_BLUE);
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_BLUE),
                new EmptyBorder(9, 16, 9, 16)
        ));
        return button;
    }

    private JButton createDangerButton(String text) {
        JButton button = createBaseButton(text);
        button.setBackground(new Color(181, 53, 53));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 17, 10, 17));
        return button;
    }

    private JButton createBaseButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(115, 42));
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
