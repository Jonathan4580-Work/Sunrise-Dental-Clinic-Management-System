-- Sunrise Dental Clinic Management System
-- Initial MySQL/MariaDB schema

CREATE DATABASE IF NOT EXISTS dental_clinic_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE dental_clinic_db;

SET NAMES utf8mb4;

-- Staff accounts used to access the clinic management system.
CREATE TABLE users (
    user_id INT UNSIGNED AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'STAFF') NOT NULL DEFAULT 'STAFF',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT uq_users_username UNIQUE (username)
) ENGINE=InnoDB;

-- Patient details are stored once and referenced by appointments.
CREATE TABLE patients (
    patient_id INT UNSIGNED AUTO_INCREMENT,
    patient_number VARCHAR(20) NOT NULL,
    first_name VARCHAR(60) NOT NULL,
    last_name VARCHAR(60) NOT NULL,
    date_of_birth DATE NULL,
    gender ENUM('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY') NULL,
    phone VARCHAR(25) NOT NULL,
    email VARCHAR(120) NULL,
    national_id VARCHAR(30) NULL,
    address_line_1 VARCHAR(150) NULL,
    address_line_2 VARCHAR(150) NULL,
    city VARCHAR(80) NULL,
    medical_notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (patient_id),
    CONSTRAINT uq_patients_patient_number UNIQUE (patient_number),
    CONSTRAINT uq_patients_email UNIQUE (email),
    CONSTRAINT uq_patients_national_id UNIQUE (national_id),
    INDEX idx_patients_name (last_name, first_name),
    INDEX idx_patients_phone (phone)
) ENGINE=InnoDB;

-- Dentists who can be allocated to appointments.
CREATE TABLE dentists (
    dentist_id INT UNSIGNED AUTO_INCREMENT,
    registration_number VARCHAR(40) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NULL,
    phone VARCHAR(25) NULL,
    email VARCHAR(120) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (dentist_id),
    CONSTRAINT uq_dentists_registration_number UNIQUE (registration_number),
    CONSTRAINT uq_dentists_email UNIQUE (email),
    INDEX idx_dentists_name (full_name)
) ENGINE=InnoDB;

-- Reusable catalogue of dental treatments and their current standard prices.
CREATE TABLE treatments (
    treatment_id INT UNSIGNED AUTO_INCREMENT,
    treatment_code VARCHAR(20) NOT NULL,
    treatment_name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    price DECIMAL(10, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (treatment_id),
    CONSTRAINT uq_treatments_code UNIQUE (treatment_code),
    CONSTRAINT uq_treatments_name UNIQUE (treatment_name),
    CONSTRAINT chk_treatments_price_nonnegative CHECK (price >= 0)
) ENGINE=InnoDB;

-- Scheduled clinic visits linking patients, dentists, treatments, and staff.
CREATE TABLE appointments (
    appointment_id BIGINT UNSIGNED AUTO_INCREMENT,
    appointment_number VARCHAR(25) NOT NULL,
    patient_id INT UNSIGNED NOT NULL,
    dentist_id INT UNSIGNED NOT NULL,
    treatment_id INT UNSIGNED NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status ENUM(
        'SCHEDULED',
        'COMPLETED',
        'CANCELLED',
        'NO_SHOW'
    ) NOT NULL DEFAULT 'SCHEDULED',
    notes TEXT NULL,
    created_by INT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (appointment_id),
    CONSTRAINT uq_appointments_number UNIQUE (appointment_number),
    CONSTRAINT uq_appointments_dentist_schedule
        UNIQUE (dentist_id, appointment_date, appointment_time),
    CONSTRAINT uq_appointments_patient_schedule
        UNIQUE (patient_id, appointment_date, appointment_time),
    CONSTRAINT fk_appointments_patient
        FOREIGN KEY (patient_id) REFERENCES patients (patient_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_dentist
        FOREIGN KEY (dentist_id) REFERENCES dentists (dentist_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_treatment
        FOREIGN KEY (treatment_id) REFERENCES treatments (treatment_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_created_by
        FOREIGN KEY (created_by) REFERENCES users (user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    INDEX idx_appointments_date_status (appointment_date, status),
    INDEX idx_appointments_treatment (treatment_id),
    INDEX idx_appointments_created_by (created_by)
) ENGINE=InnoDB;

-- Financial records. Each appointment can have no more than one bill.
CREATE TABLE bills (
    bill_id BIGINT UNSIGNED AUTO_INCREMENT,
    bill_number VARCHAR(25) NOT NULL,
    appointment_id BIGINT UNSIGNED NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(10, 2)
        GENERATED ALWAYS AS (subtotal - discount_amount + tax_amount) STORED,
    amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    payment_status ENUM(
        'UNPAID',
        'PARTIALLY_PAID',
        'PAID',
        'VOID'
    ) NOT NULL DEFAULT 'UNPAID',
    payment_method ENUM(
        'CASH',
        'CARD',
        'BANK_TRANSFER',
        'OTHER'
    ) NULL,
    paid_at DATETIME NULL,
    generated_by INT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (bill_id),
    CONSTRAINT uq_bills_number UNIQUE (bill_number),
    CONSTRAINT uq_bills_appointment UNIQUE (appointment_id),
    CONSTRAINT chk_bills_subtotal_nonnegative CHECK (subtotal >= 0),
    CONSTRAINT chk_bills_discount_nonnegative CHECK (discount_amount >= 0),
    CONSTRAINT chk_bills_discount_within_subtotal
        CHECK (discount_amount <= subtotal),
    CONSTRAINT chk_bills_tax_nonnegative CHECK (tax_amount >= 0),
    CONSTRAINT chk_bills_amount_paid_nonnegative CHECK (amount_paid >= 0),
    CONSTRAINT chk_bills_amount_paid_within_total
        CHECK (amount_paid <= total_amount),
    CONSTRAINT fk_bills_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (appointment_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_bills_generated_by
        FOREIGN KEY (generated_by) REFERENCES users (user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    INDEX idx_bills_payment_status (payment_status),
    INDEX idx_bills_created_at (created_at),
    INDEX idx_bills_generated_by (generated_by)
) ENGINE=InnoDB;

START TRANSACTION;

-- Temporary administrator account. Username: admin, password: admin
-- The password is stored as a bcrypt hash and must be changed before deployment.
INSERT INTO users (
    username,
    password_hash,
    full_name,
    role
) VALUES (
    'admin',
    '$2y$10$zGJNDcEGW1UyAaKO7QDvMe/Fr.0WLq15Nzzb/wxTqzpbr8kQUP2oa',
    'System Administrator',
    'ADMIN'
);

INSERT INTO dentists (
    registration_number,
    full_name,
    specialization,
    phone,
    email
) VALUES
    (
        'DEN-001',
        'Dr. Anjali Perera',
        'General Dentistry',
        '011-555-0101',
        'anjali.perera@sunrisedental.example'
    ),
    (
        'DEN-002',
        'Dr. Nimal Fernando',
        'Orthodontics',
        '011-555-0102',
        'nimal.fernando@sunrisedental.example'
    ),
    (
        'DEN-003',
        'Dr. Kavindi Silva',
        'Endodontics',
        '011-555-0103',
        'kavindi.silva@sunrisedental.example'
    );

INSERT INTO treatments (
    treatment_code,
    treatment_name,
    description,
    price
) VALUES
    (
        'TRT-001',
        'Dental Consultation',
        'Initial oral examination and treatment consultation.',
        2500.00
    ),
    (
        'TRT-002',
        'Teeth Cleaning',
        'Professional scaling and polishing.',
        6000.00
    ),
    (
        'TRT-003',
        'Dental Filling',
        'Composite filling for a single tooth.',
        8500.00
    ),
    (
        'TRT-004',
        'Tooth Extraction',
        'Standard non-surgical tooth extraction.',
        10000.00
    ),
    (
        'TRT-005',
        'Root Canal Treatment',
        'Root canal treatment for a single tooth.',
        35000.00
    ),
    (
        'TRT-006',
        'Teeth Whitening',
        'Professional in-clinic teeth whitening.',
        25000.00
    );

COMMIT;
