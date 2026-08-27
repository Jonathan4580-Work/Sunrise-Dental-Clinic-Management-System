package com.mycompany.sunrisedentalclinicmanagementsystem.model;

/**
 * Represents a dentist available for appointment allocation.
 */
public final class Dentist {

    private long dentistId;
    private String registrationNumber;
    private String fullName;
    private String specialization;
    private boolean active;

    public Dentist() {
    }

    public Dentist(
            long dentistId,
            String registrationNumber,
            String fullName,
            String specialization,
            boolean active
    ) {
        this.dentistId = dentistId;
        this.registrationNumber = registrationNumber;
        this.fullName = fullName;
        this.specialization = specialization;
        this.active = active;
    }

    public long getDentistId() {
        return dentistId;
    }

    public void setDentistId(long dentistId) {
        this.dentistId = dentistId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        if (specialization == null || specialization.isBlank()) {
            return fullName;
        }
        return fullName + " - " + specialization;
    }
}
