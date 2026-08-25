package com.mycompany.sunrisedentalclinicmanagementsystem;

import com.mycompany.sunrisedentalclinicmanagementsystem.service.AuthenticationService;
import com.mycompany.sunrisedentalclinicmanagementsystem.ui.LoginFrame;
import javax.swing.SwingUtilities;

/**
 * Application entry point for the Sunrise Dental Clinic Management System.
 */
public final class SunriseDentalClinicManagementSystem {

    private SunriseDentalClinicManagementSystem() {
        // Prevent creation of the application entry-point class.
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AuthenticationService authenticationService
                    = new AuthenticationService();
            LoginFrame loginFrame = new LoginFrame(authenticationService);
            loginFrame.setVisible(true);
        });
    }
}
