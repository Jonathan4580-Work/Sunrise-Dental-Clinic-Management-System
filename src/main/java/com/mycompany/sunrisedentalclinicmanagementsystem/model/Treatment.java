package com.mycompany.sunrisedentalclinicmanagementsystem.model;

import java.math.BigDecimal;

/**
 * Represents a treatment from the clinic's treatment catalogue.
 */
public final class Treatment {

    private long treatmentId;
    private String treatmentCode;
    private String treatmentName;
    private String description;
    private BigDecimal price;
    private boolean active;

    public Treatment() {
    }

    public Treatment(
            long treatmentId,
            String treatmentCode,
            String treatmentName,
            String description,
            BigDecimal price,
            boolean active
    ) {
        this.treatmentId = treatmentId;
        this.treatmentCode = treatmentCode;
        this.treatmentName = treatmentName;
        this.description = description;
        this.price = price;
        this.active = active;
    }

    public long getTreatmentId() {
        return treatmentId;
    }

    public void setTreatmentId(long treatmentId) {
        this.treatmentId = treatmentId;
    }

    public String getTreatmentCode() {
        return treatmentCode;
    }

    public void setTreatmentCode(String treatmentCode) {
        this.treatmentCode = treatmentCode;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public void setTreatmentName(String treatmentName) {
        this.treatmentName = treatmentName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return treatmentName + " (" + treatmentCode + ") - LKR "
                + price.toPlainString();
    }
}
