package com.mycompany.sunrisedentalclinicmanagementsystem.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 * Main management dashboard for Sunrise Dental Clinic.
 */
public final class DashboardFrame extends JFrame {

    private static final Color PRIMARY_BLUE = new Color(22, 91, 170);
    private static final Color DARK_BLUE = new Color(14, 55, 102);
    private static final Color SIDEBAR_BLUE = new Color(17, 68, 126);
    private static final Color SIDEBAR_HOVER = new Color(30, 92, 157);
    private static final Color TEXT_COLOR = new Color(37, 50, 66);
    private static final Color MUTED_TEXT_COLOR = new Color(105, 118, 132);
    private static final Color WINDOW_BACKGROUND = new Color(242, 247, 252);
    private static final Color CARD_BORDER_COLOR = new Color(218, 226, 234);

    private static final Font HEADER_TITLE_FONT
            = new Font("SansSerif", Font.BOLD, 22);
    private static final Font HEADER_DETAIL_FONT
            = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font SIDEBAR_BUTTON_FONT
            = new Font("SansSerif", Font.BOLD, 14);
    private static final Font WELCOME_TITLE_FONT
            = new Font("SansSerif", Font.BOLD, 27);
    private static final Font BODY_FONT
            = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font STATISTIC_VALUE_FONT
            = new Font("SansSerif", Font.BOLD, 34);

    private static final DateTimeFormatter DATE_FORMATTER
            = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER
            = DateTimeFormatter.ofPattern("hh:mm:ss a");

    private final Runnable registerAppointmentAction;
    private final Runnable logoutAction;

    private JLabel currentDateLabel;
    private JLabel currentTimeLabel;
    private JButton registerAppointmentButton;
    private JButton searchAppointmentButton;
    private JButton billingButton;
    private JButton reportsButton;
    private JButton helpButton;
    private JButton logoutButton;
    private JButton exitButton;
    private Timer clockTimer;

    /**
     * Creates and arranges the clinic management dashboard.
     *
     * @param registerAppointmentAction action that opens appointment registration
     * @param logoutAction action invoked after the dashboard closes on logout
     */
    public DashboardFrame(
            Runnable registerAppointmentAction,
            Runnable logoutAction
    ) {
        this.registerAppointmentAction = Objects.requireNonNull(
                registerAppointmentAction,
                "registerAppointmentAction must not be null"
        );
        this.logoutAction = Objects.requireNonNull(
                logoutAction,
                "logoutAction must not be null"
        );

        initializeComponents();

        JPanel dashboardPanel = new JPanel(new BorderLayout());
        dashboardPanel.setPreferredSize(new Dimension(1180, 720));
        dashboardPanel.add(createHeader(), BorderLayout.NORTH);
        dashboardPanel.add(createSidebar(), BorderLayout.WEST);
        dashboardPanel.add(createMainContent(), BorderLayout.CENTER);

        setContentPane(dashboardPanel);
        registerEvents();
        startClock();

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Creates and configures the reusable dashboard components.
     */
    private void initializeComponents() {
        setTitle("Sunrise Dental Clinic Management System - Dashboard");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(980, 620));
        setIconImage(createApplicationIcon());

        currentDateLabel = createHeaderDetailLabel();
        currentTimeLabel = createHeaderDetailLabel();

        registerAppointmentButton = createSidebarButton("Register Appointment");
        searchAppointmentButton = createSidebarButton("Search Appointment");
        billingButton = createSidebarButton("Billing");
        reportsButton = createSidebarButton("Reports");
        helpButton = createSidebarButton("Help");
        logoutButton = createSidebarButton("Logout");
        exitButton = createSidebarButton("Exit");
    }

    /**
     * Creates the top clinic header, welcome message, date, and live clock.
     */
    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout(24, 0));
        headerPanel.setBackground(DARK_BLUE);
        headerPanel.setBorder(new EmptyBorder(22, 28, 22, 30));
        headerPanel.setPreferredSize(new Dimension(0, 104));

        JLabel clinicTitleLabel = new JLabel(
                "Sunrise Dental Clinic Management System"
        );
        clinicTitleLabel.setFont(HEADER_TITLE_FONT);
        clinicTitleLabel.setForeground(Color.WHITE);

        JLabel welcomeLabel = new JLabel("Welcome, Administrator");
        welcomeLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        welcomeLabel.setForeground(new Color(218, 234, 250));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(clinicTitleLabel);
        titlePanel.add(Box.createVerticalStrut(7));
        titlePanel.add(welcomeLabel);

        JPanel dateTimePanel = new JPanel();
        dateTimePanel.setOpaque(false);
        dateTimePanel.setLayout(new BoxLayout(dateTimePanel, BoxLayout.Y_AXIS));
        currentDateLabel.setAlignmentX(RIGHT_ALIGNMENT);
        currentTimeLabel.setAlignmentX(RIGHT_ALIGNMENT);
        dateTimePanel.add(currentDateLabel);
        dateTimePanel.add(Box.createVerticalStrut(7));
        dateTimePanel.add(currentTimeLabel);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(dateTimePanel, BorderLayout.EAST);
        return headerPanel;
    }

    /**
     * Creates the left navigation sidebar.
     */
    private JPanel createSidebar() {
        JPanel sidebarPanel = new JPanel();
        sidebarPanel.setBackground(SIDEBAR_BLUE);
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBorder(new EmptyBorder(24, 16, 20, 16));
        sidebarPanel.setPreferredSize(new Dimension(245, 0));

        JLabel navigationLabel = new JLabel("NAVIGATION");
        navigationLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        navigationLabel.setForeground(new Color(174, 205, 234));
        navigationLabel.setBorder(new EmptyBorder(0, 10, 12, 0));
        navigationLabel.setAlignmentX(LEFT_ALIGNMENT);

        sidebarPanel.add(navigationLabel);
        sidebarPanel.add(registerAppointmentButton);
        sidebarPanel.add(Box.createVerticalStrut(8));
        sidebarPanel.add(searchAppointmentButton);
        sidebarPanel.add(Box.createVerticalStrut(8));
        sidebarPanel.add(billingButton);
        sidebarPanel.add(Box.createVerticalStrut(8));
        sidebarPanel.add(reportsButton);
        sidebarPanel.add(Box.createVerticalStrut(8));
        sidebarPanel.add(helpButton);
        sidebarPanel.add(Box.createVerticalGlue());
        sidebarPanel.add(logoutButton);
        sidebarPanel.add(Box.createVerticalStrut(8));
        sidebarPanel.add(exitButton);

        return sidebarPanel;
    }

    /**
     * Creates the welcome area and placeholder statistic cards.
     */
    private JPanel createMainContent() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 28));
        mainPanel.setBackground(WINDOW_BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(36, 40, 40, 40));

        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new BoxLayout(welcomePanel, BoxLayout.Y_AXIS));
        welcomePanel.setBackground(Color.WHITE);
        welcomePanel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER_COLOR),
                new EmptyBorder(28, 30, 28, 30)
        ));

        JLabel welcomeHeadingLabel = new JLabel(
                "Welcome to Sunrise Dental Clinic"
        );
        welcomeHeadingLabel.setFont(WELCOME_TITLE_FONT);
        welcomeHeadingLabel.setForeground(TEXT_COLOR);
        welcomeHeadingLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel descriptionLabel = new JLabel(
                "<html>Manage clinic appointments, patient records, billing, "
                        + "and reports from one secure workspace.</html>"
        );
        descriptionLabel.setFont(BODY_FONT);
        descriptionLabel.setForeground(MUTED_TEXT_COLOR);
        descriptionLabel.setAlignmentX(LEFT_ALIGNMENT);

        welcomePanel.add(welcomeHeadingLabel);
        welcomePanel.add(Box.createVerticalStrut(12));
        welcomePanel.add(descriptionLabel);

        JPanel statisticsSection = new JPanel(new BorderLayout(0, 16));
        statisticsSection.setOpaque(false);

        JLabel statisticsHeadingLabel = new JLabel("Clinic Overview");
        statisticsHeadingLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        statisticsHeadingLabel.setForeground(TEXT_COLOR);

        JPanel statisticCardsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statisticCardsPanel.setOpaque(false);
        statisticCardsPanel.add(createStatisticCard(
                "Today's Appointments",
                "0",
                PRIMARY_BLUE
        ));
        statisticCardsPanel.add(createStatisticCard(
                "Registered Patients",
                "0",
                new Color(32, 145, 108)
        ));
        statisticCardsPanel.add(createStatisticCard(
                "Bills Generated",
                "0",
                new Color(220, 137, 35)
        ));

        statisticsSection.add(statisticsHeadingLabel, BorderLayout.NORTH);
        statisticsSection.add(statisticCardsPanel, BorderLayout.CENTER);

        JPanel mainContentWrapper = new JPanel(new BorderLayout(0, 28));
        mainContentWrapper.setOpaque(false);
        mainContentWrapper.add(welcomePanel, BorderLayout.NORTH);
        mainContentWrapper.add(statisticsSection, BorderLayout.CENTER);

        mainPanel.add(mainContentWrapper, BorderLayout.NORTH);
        return mainPanel;
    }

    /**
     * Registers temporary navigation actions and safe closing behaviour.
     */
    private void registerEvents() {
        registerAppointmentButton.addActionListener(event ->
                handleRegisterAppointment());
        searchAppointmentButton.addActionListener(event ->
                showComingSoonDialog("Search Appointment"));
        billingButton.addActionListener(event ->
                showComingSoonDialog("Billing"));
        reportsButton.addActionListener(event ->
                showComingSoonDialog("Reports"));
        helpButton.addActionListener(event -> showHelpDialog());
        logoutButton.addActionListener(event -> handleLogout());
        exitButton.addActionListener(event -> handleExit());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                handleExit();
            }
        });
    }

    private void handleRegisterAppointment() {
        dispose();
        registerAppointmentAction.run();
    }

    private JPanel createStatisticCard(String title, String value, Color accentColor) {
        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(4, 0, 0, 0, accentColor),
                new CompoundBorder(
                        BorderFactory.createLineBorder(CARD_BORDER_COLOR),
                        new EmptyBorder(22, 24, 24, 24)
                )
        ));
        cardPanel.setPreferredSize(new Dimension(230, 150));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(STATISTIC_VALUE_FONT);
        valueLabel.setForeground(accentColor);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLabel.setForeground(MUTED_TEXT_COLOR);

        cardPanel.add(valueLabel, BorderLayout.CENTER);
        cardPanel.add(titleLabel, BorderLayout.SOUTH);
        return cardPanel;
    }

    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setFont(SIDEBAR_BUTTON_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(SIDEBAR_BLUE);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(13, 14, 13, 14));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setAlignmentX(LEFT_ALIGNMENT);

        button.addChangeListener(event -> {
            if (button.getModel().isRollover()) {
                button.setBackground(SIDEBAR_HOVER);
            } else {
                button.setBackground(SIDEBAR_BLUE);
            }
        });

        return button;
    }

    private JLabel createHeaderDetailLabel() {
        JLabel label = new JLabel();
        label.setFont(HEADER_DETAIL_FONT);
        label.setForeground(new Color(218, 234, 250));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    private void startClock() {
        updateDateAndTime();
        clockTimer = new Timer(1_000, event -> updateDateAndTime());
        clockTimer.start();
    }

    private void updateDateAndTime() {
        currentDateLabel.setText(LocalDate.now().format(DATE_FORMATTER));
        currentTimeLabel.setText(LocalTime.now().format(TIME_FORMATTER));
    }

    private void showComingSoonDialog(String featureName) {
        JOptionPane.showMessageDialog(
                this,
                featureName + " is currently under development.\n"
                        + "This feature will be available soon.",
                "Feature Coming Soon",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showHelpDialog() {
        JOptionPane.showMessageDialog(
                this,
                "Sunrise Dental Clinic Management System\n\n"
                        + "Use the sidebar to access clinic management features.\n"
                        + "The available modules will be implemented in future stages.",
                "Help",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void handleLogout() {
        dispose();
        logoutAction.run();
    }

    private void handleExit() {
        int selectedOption = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit the application?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (selectedOption == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    /**
     * Stops the live clock before releasing this window's resources.
     */
    @Override
    public void dispose() {
        if (clockTimer != null) {
            clockTimer.stop();
        }
        super.dispose();
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
