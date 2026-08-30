package com.mycompany.sunrisedentalclinicmanagementsystem.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 * Displays step-by-step guidance for clinic staff.
 */
public final class HelpFrame extends JFrame {

    private static final Color PRIMARY_BLUE = new Color(22, 91, 170);
    private static final Color DARK_BLUE = new Color(14, 55, 102);
    private static final Color TEXT_COLOR = new Color(37, 50, 66);
    private static final Color WINDOW_BACKGROUND = new Color(242, 247, 252);
    private static final Color CARD_BORDER_COLOR = new Color(218, 226, 234);

    private static final Font HEADER_TITLE_FONT
            = new Font("SansSerif", Font.BOLD, 25);
    private static final Font PAGE_TITLE_FONT
            = new Font("SansSerif", Font.BOLD, 20);
    private static final Font BODY_FONT
            = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font BUTTON_FONT
            = new Font("SansSerif", Font.BOLD, 13);

    private final Runnable returnToDashboardAction;

    private JTextArea helpTextArea;
    private JButton backButton;
    private boolean returningToDashboard;

    public HelpFrame(Runnable returnToDashboardAction) {
        this.returnToDashboardAction = Objects.requireNonNull(
                returnToDashboardAction,
                "returnToDashboardAction must not be null"
        );

        initializeComponents();
        setupLayout();
        registerEvents();
        pack();
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        setTitle("Sunrise Dental Clinic Management System - Help");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setIconImage(createApplicationIcon());

        helpTextArea = new JTextArea(createHelpContent());
        helpTextArea.setEditable(false);
        helpTextArea.setLineWrap(true);
        helpTextArea.setWrapStyleWord(true);
        helpTextArea.setFont(BODY_FONT);
        helpTextArea.setForeground(TEXT_COLOR);
        helpTextArea.setBackground(Color.WHITE);
        helpTextArea.setMargin(new java.awt.Insets(20, 24, 20, 24));
        helpTextArea.setCaretPosition(0);

        backButton = new JButton("Back to Dashboard");
        backButton.setFont(BUTTON_FONT);
        backButton.setForeground(Color.WHITE);
        backButton.setBackground(PRIMARY_BLUE);
        backButton.setFocusPainted(false);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.setPreferredSize(new Dimension(170, 42));
        backButton.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
    }

    private void setupLayout() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setPreferredSize(new Dimension(920, 720));
        rootPanel.setBackground(WINDOW_BACKGROUND);
        rootPanel.add(createHeader(), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(helpTextArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(CARD_BORDER_COLOR));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(18, 22, 18, 22));
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        rootPanel.add(contentPanel, BorderLayout.CENTER);
        rootPanel.add(createFooter(), BorderLayout.SOUTH);
        setContentPane(rootPanel);
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(DARK_BLUE);
        headerPanel.setBorder(new EmptyBorder(18, 24, 18, 24));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel clinicLabel = new JLabel(
                "Sunrise Dental Clinic Management System"
        );
        clinicLabel.setFont(HEADER_TITLE_FONT);
        clinicLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Staff Help Guide");
        titleLabel.setFont(PAGE_TITLE_FONT);
        titleLabel.setForeground(new Color(225, 238, 252));

        JLabel introductionLabel = new JLabel(
                "Step-by-step instructions for using the clinic system safely."
        );
        introductionLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        introductionLabel.setForeground(new Color(195, 218, 242));

        headerPanel.add(clinicLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(3));
        headerPanel.add(introductionLabel);
        return headerPanel;
    }

    private JPanel createFooter() {
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER_COLOR),
                new EmptyBorder(13, 22, 13, 22)
        ));
        footerPanel.add(backButton, BorderLayout.WEST);
        return footerPanel;
    }

    private void registerEvents() {
        backButton.addActionListener(event -> returnToDashboard());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                returnToDashboard();
            }
        });
    }

    private void returnToDashboard() {
        if (returningToDashboard) {
            return;
        }

        returningToDashboard = true;
        dispose();
        returnToDashboardAction.run();
    }

    private String createHelpContent() {
        return """
                WELCOME

                This guide explains the current Sunrise Dental Clinic workflow.
                Follow each section in order when learning the system.

                1. LOGGING IN

                1. Enter your authorised username in the Username field.
                2. Enter your password in the Password field.
                3. Click Login, or press Enter after typing the password.
                4. If the details are correct, the Dashboard opens.
                5. If an error appears, check both fields and try again.

                2. REGISTERING A NEW APPOINTMENT

                1. From the Dashboard, click Register Appointment.
                2. Enter the patient's first name, last name, address, city and contact number.
                3. Enter a unique Appointment Number.
                4. Select a Dentist and Treatment from the available lists.
                5. Select the appointment date and time, then add notes if required.
                6. Review the treatment price and all entered information.
                7. Click Register Appointment.
                8. Wait for the success message containing the appointment number.

                3. SEARCHING FOR AN APPOINTMENT

                1. From the Dashboard, click Search Appointment.
                2. Enter the Appointment Number in the search field.
                3. Click Search, or press Enter.
                4. The patient and appointment information appears in read-only mode.
                5. If no record exists, check the number and search again.

                4. EDITING AN APPOINTMENT

                1. Search for the appointment using its Appointment Number.
                2. Click Edit.
                3. Change only the patient or appointment fields that need correction.
                4. Click Save Changes.
                5. Wait for the success message and refreshed appointment details.
                6. Cancelled appointments cannot be edited.

                5. CANCELLING AN APPOINTMENT

                1. Search for the appointment using its Appointment Number.
                2. Click Cancel Appointment.
                3. Read the confirmation message carefully.
                4. Choose Yes only when the appointment should be cancelled.
                5. The appointment remains in the system with status CANCELLED.

                6. CALCULATING AND GENERATING A BILL

                1. From the Dashboard, click Billing.
                2. Enter the Appointment Number.
                3. Click Search / Calculate, or press Enter.
                4. Review the patient, appointment, treatment and charge information.
                5. Check the treatment price, consultation fee and Final Total.
                6. Click Generate Bill once.
                7. Record the bill number shown in the success message.
                8. Only one bill can be generated for each appointment.

                7. PRINTING A RECEIPT

                1. Generate the bill successfully first.
                2. Confirm that the final receipt preview and bill number are visible.
                3. Click Print Receipt.
                4. Select a printer, or choose Microsoft Print to PDF.
                5. Click Print in the system print dialog.
                6. Cancelling the print dialog does not change the saved bill.

                8. RETURNING TO THE DASHBOARD

                1. Click Back to Dashboard from Register Appointment, Search Appointment,
                   Billing or Help.
                2. Closing one of these feature windows also returns safely to the Dashboard.
                3. Your current authenticated session remains active.

                9. SAFELY EXITING THE SYSTEM

                1. Return to the Dashboard.
                2. Click Exit, or use the Dashboard window-close button.
                3. At the confirmation message, choose Yes to close the system.
                4. Choose No to keep the Dashboard open and continue working.
                5. Always finish saving or printing before exiting.
                """;
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
