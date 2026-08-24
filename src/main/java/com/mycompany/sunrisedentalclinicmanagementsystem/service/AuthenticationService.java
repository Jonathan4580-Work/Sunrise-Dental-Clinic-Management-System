package com.mycompany.sunrisedentalclinicmanagementsystem.service;

/**
 * Provides user authentication for the application.
 *
 * <p>This initial implementation uses temporary credentials. It can later be
 * replaced with DAO-based authentication without placing authentication rules
 * inside the user interface.</p>
 */
public final class AuthenticationService {

    private static final String TEMPORARY_USERNAME = "admin";
    private static final String TEMPORARY_PASSWORD = "admin";

    /**
     * Checks whether the supplied credentials match the temporary clinic
     * administrator credentials.
     *
     * @param username username entered by the user
     * @param password password entered by the user
     * @return {@code true} when both credentials are valid; otherwise
     *         {@code false}
     */
    public boolean authenticate(String username, String password) {
        return TEMPORARY_USERNAME.equals(username)
                && TEMPORARY_PASSWORD.equals(password);
    }
}
