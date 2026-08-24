package com.mycompany.sunrisedentalclinicmanagementsystem.ui;

import com.mycompany.sunrisedentalclinicmanagementsystem.service.AuthenticationService;
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
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 * Presents the secure staff login interface for Sunrise Dental Clinic.
 */
public final class LoginFrame extends JFrame {

    private static final Color PRIMARY_BLUE = new Color(22, 91, 170);
    private static final Color DARK_BLUE = new Color(14, 55, 102);
    private static final Color TEXT_COLOR = new Color(37, 50, 66);
    private static final Color MUTED_TEXT_COLOR = new Color(105, 118, 132);
    private static final Color FIELD_BORDER_COLOR = new Color(202, 213, 224);
    private static final Color WINDOW_BACKGROUND = new Color(242, 247, 252);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 27);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 15);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 13);
    private static final Font INPUT_FONT = new Font("SansSerif", Font.PLAIN, 15);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);

    private final AuthenticationService authenticationService;

    private JLabel clinicTitleLabel;
    private JLabel subtitleLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JTextField usernameTextField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton exitButton;
    private JPanel headerPanel;
    private JPanel formPanel;

    /**
     * Creates a login window backed by the supplied authentication service.
     *
     * @param authenticationService service used to validate staff credentials
     */
    public LoginFrame(AuthenticationService authenticationService) {
        this.authenticationService = Objects.requireNonNull(
                authenticationService,
                "authenticationService must not be null"
        );

        initializeComponents();
        setupLayout();
        registerEvents();
    }

    /**
     * Creates and configures the individual Swing components.
     */
    private void initializeComponents() {
        setTitle("Sunrise Dental Clinic Management System");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setIconImage(createApplicationIcon());

        clinicTitleLabel = new JLabel("Sunrise Dental Clinic", SwingConstants.CENTER);
        clinicTitleLabel.setFont(TITLE_FONT);
        clinicTitleLabel.setForeground(Color.WHITE);

        subtitleLabel = new JLabel("Secure Staff Login", SwingConstants.CENTER);
        subtitleLabel.setFont(SUBTITLE_FONT);
        subtitleLabel.setForeground(new Color(218, 234, 250));

        usernameLabel = createFormLabel("Username");
        passwordLabel = createFormLabel("Password");

        usernameTextField = createTextField();
        passwordField = new JPasswordField();
        styleInputField(passwordField);

        loginButton = createPrimaryButton("Login");
        exitButton = createSecondaryButton("Exit");

        getRootPane().setDefaultButton(loginButton);
    }

    /**
     * Arranges the header and login form with consistent spacing and margins.
     */
    private void setupLayout() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setPreferredSize(new Dimension(520, 570));
        contentPanel.setBackground(WINDOW_BACKGROUND);

        headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBackground(DARK_BLUE);
        headerPanel.setBorder(new EmptyBorder(34, 40, 34, 40));

        GridBagConstraints headerConstraints = new GridBagConstraints();
        headerConstraints.gridx = 0;
        headerConstraints.gridy = 0;
        headerConstraints.fill = GridBagConstraints.HORIZONTAL;
        headerConstraints.weightx = 1.0;
        headerPanel.add(clinicTitleLabel, headerConstraints);

        headerConstraints.gridy = 1;
        headerConstraints.insets = new Insets(8, 0, 0, 0);
        headerPanel.add(subtitleLabel, headerConstraints);

        formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(38, 54, 42, 54));

        GridBagConstraints formConstraints = new GridBagConstraints();
        formConstraints.gridx = 0;
        formConstraints.fill = GridBagConstraints.HORIZONTAL;
        formConstraints.weightx = 1.0;

        formConstraints.gridy = 0;
        formConstraints.insets = new Insets(0, 0, 8, 0);
        formPanel.add(usernameLabel, formConstraints);

        formConstraints.gridy = 1;
        formConstraints.insets = new Insets(0, 0, 24, 0);
        formPanel.add(usernameTextField, formConstraints);

        formConstraints.gridy = 2;
        formConstraints.insets = new Insets(0, 0, 8, 0);
        formPanel.add(passwordLabel, formConstraints);

        formConstraints.gridy = 3;
        formConstraints.insets = new Insets(0, 0, 32, 0);
        formPanel.add(passwordField, formConstraints);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 14, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(exitButton);
        buttonPanel.add(loginButton);

        formConstraints.gridy = 4;
        formConstraints.insets = new Insets(0, 0, 0, 0);
        formPanel.add(buttonPanel, formConstraints);

        JPanel formContainer = new JPanel(new BorderLayout());
        formContainer.setBackground(WINDOW_BACKGROUND);
        formContainer.setBorder(new EmptyBorder(24, 28, 28, 28));
        formContainer.add(formPanel, BorderLayout.CENTER);

        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(formContainer, BorderLayout.CENTER);
        setContentPane(contentPanel);

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Connects user actions to presentation handlers.
     */
    private void registerEvents() {
        loginButton.addActionListener(event -> handleLogin());
        passwordField.addActionListener(event -> handleLogin());
        exitButton.addActionListener(event -> handleExit());
    }

    private void handleLogin() {
        String username = usernameTextField.getText().trim();
        char[] passwordCharacters = passwordField.getPassword();

        if (username.isEmpty()) {
            clearPasswordCharacters(passwordCharacters);
            showValidationError("Please enter your username.");
            usernameTextField.requestFocusInWindow();
            return;
        }

        if (passwordCharacters.length == 0) {
            showValidationError("Please enter your password.");
            passwordField.requestFocusInWindow();
            return;
        }

        String password = new String(passwordCharacters);
        clearPasswordCharacters(passwordCharacters);

        if (authenticationService.authenticate(username, password)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Login Successful",
                    "Authentication Successful",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        passwordField.setText("");
        JOptionPane.showMessageDialog(
                this,
                "The username or password you entered is incorrect.\n"
                        + "Please check your credentials and try again.",
                "Login Failed",
                JOptionPane.ERROR_MESSAGE
        );
        passwordField.requestFocusInWindow();
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

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Required Information",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_COLOR);
        return label;
    }

    private JTextField createTextField() {
        JTextField textField = new JTextField();
        styleInputField(textField);
        return textField;
    }

    private void styleInputField(JTextField textField) {
        textField.setFont(INPUT_FONT);
        textField.setForeground(TEXT_COLOR);
        textField.setCaretColor(PRIMARY_BLUE);
        textField.setPreferredSize(new Dimension(360, 44));
        textField.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER_COLOR, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
    }

    private JButton createPrimaryButton(String text) {
        JButton button = createBaseButton(text);
        button.setBackground(PRIMARY_BLUE);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = createBaseButton(text);
        button.setBackground(Color.WHITE);
        button.setForeground(DARK_BLUE);
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_BLUE, 1),
                new EmptyBorder(11, 20, 11, 20)
        ));
        return button;
    }

    private JButton createBaseButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 46));
        return button;
    }

    private void clearPasswordCharacters(char[] passwordCharacters) {
        Arrays.fill(passwordCharacters, '\0');
    }

    /**
     * Creates a simple built-in placeholder icon until clinic artwork is added.
     */
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
            graphics.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            graphics.drawLine(16, 8, 16, 24);
            graphics.drawLine(8, 16, 24, 16);
        } finally {
            graphics.dispose();
        }

        return icon;
    }
}
