package com.mycompany.sunrisedentalclinicmanagementsystem.controller;

import com.mycompany.sunrisedentalclinicmanagementsystem.dao.DentistDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.dao.TreatmentDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Dentist;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Treatment;
import com.mycompany.sunrisedentalclinicmanagementsystem.service.AppointmentRegistrationException;
import com.mycompany.sunrisedentalclinicmanagementsystem.service.AppointmentRegistrationRequest;
import com.mycompany.sunrisedentalclinicmanagementsystem.service.AppointmentRegistrationService;
import com.mycompany.sunrisedentalclinicmanagementsystem.ui.RegisterAppointmentFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Coordinates appointment-registration UI events, reference data, and the
 * registration service.
 */
public final class RegisterAppointmentController {

    private final RegisterAppointmentFrame frame;
    private final AppointmentRegistrationService registrationService;
    private final DentistDAO dentistDAO;
    private final TreatmentDAO treatmentDAO;
    private final String authenticatedUsername;
    private final Runnable returnToDashboardAction;

    private boolean referenceDataLoaded;
    private boolean returningToDashboard;

    /**
     * Creates the appointment-registration feature for the current staff user.
     *
     * @param authenticatedUsername username that will own the audit record
     * @param returnToDashboardAction action invoked when registration closes
     */
    public RegisterAppointmentController(
            String authenticatedUsername,
            Runnable returnToDashboardAction
    ) {
        this(
                new RegisterAppointmentFrame(),
                new AppointmentRegistrationService(),
                new DentistDAO(),
                new TreatmentDAO(),
                authenticatedUsername,
                returnToDashboardAction
        );
    }

    RegisterAppointmentController(
            RegisterAppointmentFrame frame,
            AppointmentRegistrationService registrationService,
            DentistDAO dentistDAO,
            TreatmentDAO treatmentDAO,
            String authenticatedUsername,
            Runnable returnToDashboardAction
    ) {
        this.frame = Objects.requireNonNull(frame, "frame must not be null");
        this.registrationService = Objects.requireNonNull(
                registrationService,
                "registrationService must not be null"
        );
        this.dentistDAO = Objects.requireNonNull(
                dentistDAO,
                "dentistDAO must not be null"
        );
        this.treatmentDAO = Objects.requireNonNull(
                treatmentDAO,
                "treatmentDAO must not be null"
        );
        this.authenticatedUsername = Objects.requireNonNull(
                authenticatedUsername,
                "authenticatedUsername must not be null"
        );
        this.returnToDashboardAction = Objects.requireNonNull(
                returnToDashboardAction,
                "returnToDashboardAction must not be null"
        );

        registerEvents();
    }

    /**
     * Displays the registration form and loads its database-backed selectors.
     */
    public void show() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::show);
            return;
        }

        frame.setVisible(true);
        if (!referenceDataLoaded) {
            loadReferenceData();
        }
    }

    private void registerEvents() {
        frame.addRegisterListener(event -> submitRegistration());
        frame.addClearListener(event -> {
            frame.clearForm();
            frame.setStatus("Ready", false);
        });
        frame.addCancelListener(event -> returnToDashboard());
        frame.addTreatmentSelectionListener(event -> updateTreatmentPrice());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                returnToDashboard();
            }
        });
    }

    private void loadReferenceData() {
        frame.setBusy(true, "Loading dentists and treatments...");

        new SwingWorker<ReferenceData, Void>() {
            @Override
            protected ReferenceData doInBackground() throws SQLException {
                return new ReferenceData(
                        dentistDAO.findAllActive(),
                        treatmentDAO.findAllActive()
                );
            }

            @Override
            protected void done() {
                try {
                    ReferenceData referenceData = get();
                    frame.setDentists(referenceData.dentists());
                    frame.setTreatments(referenceData.treatments());
                    referenceDataLoaded = true;
                    frame.setBusy(false, "Ready");

                    if (referenceData.dentists().isEmpty()
                            || referenceData.treatments().isEmpty()) {
                        frame.showWarningMessage(
                                "Reference Data Unavailable",
                                "At least one active dentist and treatment are "
                                        + "required before an appointment can be registered."
                        );
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showReferenceDataFailure();
                } catch (ExecutionException exception) {
                    showReferenceDataFailure();
                }
            }
        }.execute();
    }

    private void submitRegistration() {
        AppointmentRegistrationRequest request = createRegistrationRequest();
        frame.setBusy(true, "Saving appointment...");

        new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground()
                    throws AppointmentRegistrationException {
                return registrationService.register(request);
            }

            @Override
            protected void done() {
                try {
                    get();
                    frame.setBusy(false, "Appointment registered successfully.");
                    String appointmentNumber = request.appointmentNumber()
                            .trim()
                            .toUpperCase(Locale.ROOT);
                    frame.showSuccessMessage(
                            "Appointment registered successfully.\n\n"
                                    + "Appointment Number: " + appointmentNumber
                    );
                    frame.clearForm();
                    frame.setStatus("Ready", false);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    frame.setBusy(false, "Registration interrupted.");
                    frame.showErrorMessage(
                            "Registration Interrupted",
                            "Appointment registration was interrupted. Please try again."
                    );
                } catch (ExecutionException exception) {
                    frame.setBusy(false, "Appointment registration failed.");
                    handleRegistrationFailure(exception.getCause());
                }
            }
        }.execute();
    }

    private AppointmentRegistrationRequest createRegistrationRequest() {
        Dentist dentist = frame.getSelectedDentist();
        Treatment treatment = frame.getSelectedTreatment();

        return new AppointmentRegistrationRequest(
                frame.getAppointmentNumber(),
                frame.getPatientFirstName(),
                frame.getPatientLastName(),
                frame.getAddressLine1(),
                frame.getAddressLine2(),
                frame.getCity(),
                frame.getContactNumber(),
                dentist == null ? 0 : dentist.getDentistId(),
                treatment == null ? 0 : treatment.getTreatmentId(),
                frame.getAppointmentDate(),
                frame.getAppointmentTime(),
                frame.getNotes(),
                authenticatedUsername
        );
    }

    private void updateTreatmentPrice() {
        Treatment treatment = frame.getSelectedTreatment();
        frame.setTreatmentPrice(treatment == null ? null : treatment.getPrice());
    }

    private void handleRegistrationFailure(Throwable failure) {
        if (failure instanceof AppointmentRegistrationException exception) {
            frame.showErrorMessage(
                    errorTitle(exception.getReason()),
                    exception.getMessage()
            );
            return;
        }

        frame.showErrorMessage(
                "Registration Failed",
                "The appointment could not be registered. Please try again."
        );
    }

    private String errorTitle(AppointmentRegistrationException.Reason reason) {
        return switch (reason) {
            case VALIDATION -> "Check Appointment Details";
            case DUPLICATE_APPOINTMENT_NUMBER -> "Duplicate Appointment Number";
            case DENTIST_SCHEDULE_CONFLICT -> "Dentist Unavailable";
            case PATIENT_SCHEDULE_CONFLICT -> "Patient Schedule Conflict";
            case INACTIVE_DENTIST -> "Dentist Unavailable";
            case INACTIVE_TREATMENT -> "Treatment Unavailable";
            case INACTIVE_USER -> "Staff Account Unavailable";
            case DATABASE_FAILURE -> "Database Unavailable";
        };
    }

    private void showReferenceDataFailure() {
        frame.setBusy(false, "Unable to load dentists and treatments.");
        frame.setStatus("Reference data could not be loaded.", true);
        frame.showErrorMessage(
                "Unable to Load Appointment Data",
                "Dentists and treatments could not be loaded. Confirm that "
                        + "XAMPP MySQL is running, then reopen this window."
        );
    }

    private void returnToDashboard() {
        if (returningToDashboard) {
            return;
        }

        returningToDashboard = true;
        frame.dispose();
        returnToDashboardAction.run();
    }

    private record ReferenceData(
            List<Dentist> dentists,
            List<Treatment> treatments
    ) {
    }

}
