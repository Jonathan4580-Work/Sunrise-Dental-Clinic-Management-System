package com.mycompany.sunrisedentalclinicmanagementsystem;

import com.mycompany.sunrisedentalclinicmanagementsystem.controller.NavigationController;
import com.mycompany.sunrisedentalclinicmanagementsystem.service.AuthenticationService;
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
            NavigationController navigationController
                    = new NavigationController(authenticationService);
            navigationController.showLogin();
        });
    }
}
