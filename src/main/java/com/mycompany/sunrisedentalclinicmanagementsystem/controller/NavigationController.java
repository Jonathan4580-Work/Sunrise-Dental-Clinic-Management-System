package com.mycompany.sunrisedentalclinicmanagementsystem.controller;

import com.mycompany.sunrisedentalclinicmanagementsystem.service.AuthenticationService;
import com.mycompany.sunrisedentalclinicmanagementsystem.ui.DashboardFrame;
import com.mycompany.sunrisedentalclinicmanagementsystem.ui.LoginFrame;
import java.util.Objects;
import javax.swing.SwingUtilities;

/**
 * Coordinates navigation between the application's top-level windows.
 */
public final class NavigationController {

    private final AuthenticationService authenticationService;

    /**
     * Creates a navigation controller that reuses one authentication service
     * throughout the application session.
     *
     * @param authenticationService service supplied to each login window
     */
    public NavigationController(AuthenticationService authenticationService) {
        this.authenticationService = Objects.requireNonNull(
                authenticationService,
                "authenticationService must not be null"
        );
    }

    /**
     * Opens the login window.
     */
    public void showLogin() {
        runOnEventDispatchThread(() -> {
            LoginFrame loginFrame = new LoginFrame(
                    authenticationService,
                    this::showDashboard
            );
            loginFrame.setVisible(true);
        });
    }

    private void showDashboard(String authenticatedUsername) {
        runOnEventDispatchThread(() -> {
            DashboardFrame dashboardFrame = new DashboardFrame(
                    () -> showRegisterAppointment(authenticatedUsername),
                    this::showLogin
            );
            dashboardFrame.setVisible(true);
        });
    }

    private void showRegisterAppointment(String authenticatedUsername) {
        runOnEventDispatchThread(() -> {
            RegisterAppointmentController registrationController
                    = new RegisterAppointmentController(
                            authenticatedUsername,
                            () -> showDashboard(authenticatedUsername)
                    );
            registrationController.show();
        });
    }

    private void runOnEventDispatchThread(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }

        SwingUtilities.invokeLater(action);
    }
}
