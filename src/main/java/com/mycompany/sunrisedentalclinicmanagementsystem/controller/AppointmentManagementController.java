package com.mycompany.sunrisedentalclinicmanagementsystem.controller;

import com.mycompany.sunrisedentalclinicmanagementsystem.dao.DentistDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.dao.TreatmentDAO;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.AppointmentDetails;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Dentist;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.Treatment;
import com.mycompany.sunrisedentalclinicmanagementsystem.service.AppointmentManagementException;
import com.mycompany.sunrisedentalclinicmanagementsystem.service.AppointmentManagementService;
import com.mycompany.sunrisedentalclinicmanagementsystem.service.AppointmentUpdateRequest;
import com.mycompany.sunrisedentalclinicmanagementsystem.ui.SearchAppointmentFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Coordinates appointment search, editing, soft cancellation, and UI state.
 */
public final class AppointmentManagementController {

    private final SearchAppointmentFrame frame;
    private final AppointmentManagementService managementService;
    private final DentistDAO dentistDAO;
    private final TreatmentDAO treatmentDAO;
    private final Runnable returnToDashboardAction;

    private AppointmentDetails currentDetails;
    private boolean referenceDataLoaded;
    private boolean returningToDashboard;

    /**
     * Creates the appointment-management feature.
     *
     * @param returnToDashboardAction navigation callback used by Back/close
     */
    public AppointmentManagementController(Runnable returnToDashboardAction) {
        this(
                new SearchAppointmentFrame(),
                new AppointmentManagementService(),
                new DentistDAO(),
                new TreatmentDAO(),
                returnToDashboardAction
        );
    }

    AppointmentManagementController(
            SearchAppointmentFrame frame,
            AppointmentManagementService managementService,
            DentistDAO dentistDAO,
            TreatmentDAO treatmentDAO,
            Runnable returnToDashboardAction
    ) {
        this.frame = Objects.requireNonNull(frame, "frame must not be null");
        this.managementService = Objects.requireNonNull(
                managementService,
                "managementService must not be null"
        );
        this.dentistDAO = Objects.requireNonNull(
                dentistDAO,
                "dentistDAO must not be null"
        );
        this.treatmentDAO = Objects.requireNonNull(
                treatmentDAO,
                "treatmentDAO must not be null"
        );
        this.returnToDashboardAction = Objects.requireNonNull(
                returnToDashboardAction,
                "returnToDashboardAction must not be null"
        );

        registerEvents();
    }

    /**
     * Displays the frame and loads database-backed selector values.
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
        frame.addSearchListener(event -> search());
        frame.addEditListener(event -> enterEditMode());
        frame.addSaveListener(event -> saveChanges());
        frame.addCancelEditListener(event -> cancelEdit());
        frame.addCancelAppointmentListener(event -> cancelAppointment());
        frame.addClearListener(event -> clear());
        frame.addBackListener(event -> returnToDashboard());
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
                    frame.setReferenceDataAvailable(true);
                    frame.setBusy(false, "Ready");

                    if (referenceData.dentists().isEmpty()
                            || referenceData.treatments().isEmpty()) {
                        frame.setReferenceDataAvailable(false);
                        frame.showWarningMessage(
                                "Reference Data Unavailable",
                                "At least one active dentist and treatment are "
                                        + "required before an appointment can be edited."
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

    private void search() {
        performSearch(frame.getSearchAppointmentNumber(), true, true);
    }

    private void performSearch(
            String appointmentNumber,
            boolean clearBeforeSearch,
            boolean showNotFoundDialog
    ) {
        if (clearBeforeSearch) {
            currentDetails = null;
            frame.clearAppointmentDetails();
        }
        frame.setBusy(true, "Searching...");

        new SwingWorker<Optional<AppointmentDetails>, Void>() {
            @Override
            protected Optional<AppointmentDetails> doInBackground()
                    throws AppointmentManagementException {
                return managementService.searchByAppointmentNumber(
                        appointmentNumber
                );
            }

            @Override
            protected void done() {
                try {
                    Optional<AppointmentDetails> searchResult = get();
                    if (searchResult.isEmpty()) {
                        currentDetails = null;
                        frame.clearAppointmentDetails();
                        frame.setBusy(false, "Appointment not found.");
                        frame.setStatus("Appointment not found.", true);
                        if (showNotFoundDialog) {
                            frame.showInformationMessage(
                                    "Appointment Not Found",
                                    "No appointment was found with that appointment number."
                            );
                        }
                        return;
                    }

                    currentDetails = searchResult.get();
                    frame.displayAppointmentDetails(currentDetails);
                    String status = "CANCELLED".equals(currentDetails.status())
                            ? "Appointment found — this appointment is cancelled."
                            : "Appointment found.";
                    frame.setBusy(false, status);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showInterruptedFailure("Search interrupted.");
                } catch (ExecutionException exception) {
                    currentDetails = null;
                    frame.clearAppointmentDetails();
                    frame.setBusy(false, "Search failed.");
                    handleFailure(exception.getCause());
                }
            }
        }.execute();
    }

    private void enterEditMode() {
        if (currentDetails == null) {
            return;
        }
        if (!referenceDataLoaded) {
            frame.showWarningMessage(
                    "Editing Unavailable",
                    "Dentists and treatments are not available. Please try again."
            );
            return;
        }

        frame.setEditMode(true);
        frame.setStatus("Editing appointment details...", false);
    }

    private void cancelEdit() {
        if (currentDetails == null) {
            return;
        }
        frame.displayAppointmentDetails(currentDetails);
        frame.setStatus("Changes discarded.", false);
    }

    private void saveChanges() {
        if (currentDetails == null) {
            return;
        }

        AppointmentUpdateRequest request = createUpdateRequest();
        String appointmentNumber = currentDetails.appointmentNumber();
        frame.setBusy(true, "Saving changes...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground()
                    throws AppointmentManagementException {
                managementService.update(request);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    frame.showSuccessMessage("Appointment updated successfully.");
                    performSearch(appointmentNumber, false, false);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showInterruptedFailure("Update interrupted.");
                } catch (ExecutionException exception) {
                    frame.setBusy(false, "Update failed.");
                    handleFailure(exception.getCause());
                }
            }
        }.execute();
    }

    private AppointmentUpdateRequest createUpdateRequest() {
        Dentist dentist = frame.getSelectedDentist();
        Treatment treatment = frame.getSelectedTreatment();

        return new AppointmentUpdateRequest(
                currentDetails.appointmentId(),
                currentDetails.patientId(),
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
                frame.getNotes()
        );
    }

    private void cancelAppointment() {
        if (currentDetails == null || !frame.confirmCancellation()) {
            return;
        }

        long appointmentId = currentDetails.appointmentId();
        String appointmentNumber = currentDetails.appointmentNumber();
        frame.setBusy(true, "Cancelling appointment...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground()
                    throws AppointmentManagementException {
                managementService.cancel(appointmentId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    frame.showSuccessMessage("Appointment cancelled successfully.");
                    performSearch(appointmentNumber, false, false);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showInterruptedFailure("Cancellation interrupted.");
                } catch (ExecutionException exception) {
                    frame.setBusy(false, "Cancellation failed.");
                    handleFailure(exception.getCause());
                }
            }
        }.execute();
    }

    private void clear() {
        currentDetails = null;
        frame.clearForm();
    }

    private void updateTreatmentPrice() {
        Treatment treatment = frame.getSelectedTreatment();
        frame.setTreatmentPrice(treatment == null ? null : treatment.getPrice());
    }

    private void handleFailure(Throwable failure) {
        if (failure instanceof AppointmentManagementException exception) {
            if (exception.getReason()
                    == AppointmentManagementException.Reason.ALREADY_CANCELLED) {
                frame.showInformationMessage(
                        "Appointment Already Cancelled",
                        exception.getMessage()
                );
                if (currentDetails != null) {
                    performSearch(
                            currentDetails.appointmentNumber(),
                            false,
                            false
                    );
                }
                return;
            }

            frame.showErrorMessage(
                    errorTitle(exception.getReason()),
                    exception.getMessage()
            );
            return;
        }

        frame.showErrorMessage(
                "Appointment Operation Failed",
                "The requested operation could not be completed. Please try again."
        );
    }

    private String errorTitle(AppointmentManagementException.Reason reason) {
        return switch (reason) {
            case VALIDATION -> "Check Appointment Details";
            case NOT_FOUND -> "Appointment Not Found";
            case ALREADY_CANCELLED -> "Appointment Already Cancelled";
            case INACTIVE_DENTIST -> "Dentist Unavailable";
            case INACTIVE_TREATMENT -> "Treatment Unavailable";
            case DENTIST_SCHEDULE_CONFLICT -> "Dentist Unavailable";
            case PATIENT_SCHEDULE_CONFLICT -> "Patient Schedule Conflict";
            case DATABASE_FAILURE -> "Database Unavailable";
        };
    }

    private void showInterruptedFailure(String status) {
        frame.setBusy(false, status);
        frame.showErrorMessage(
                "Operation Interrupted",
                "The operation was interrupted. Please try again."
        );
    }

    private void showReferenceDataFailure() {
        referenceDataLoaded = false;
        frame.setReferenceDataAvailable(false);
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

    /**
     * Development preview entry point; it does not change application startup.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new AppointmentManagementController(() -> { }).show());
    }
}
