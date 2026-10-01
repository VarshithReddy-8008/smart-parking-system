-- ============================================================================
-- Smart Vehicle Parking Management System Database Schema (MySQL 8.0+)
-- ============================================================================

-- Create Database if not exists (Optional, typical in local setups)
CREATE DATABASE IF NOT EXISTS smart_parking_db;
USE smart_parking_db;

-- Drop tables in reverse dependency order
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS parking_sessions;
DROP TABLE IF EXISTS parking_rates;
DROP TABLE IF EXISTS parking_slots;
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS vehicle_types;
DROP TABLE IF EXISTS pricing_rules;
DROP TABLE IF EXISTS weather_status;
DROP TABLE IF EXISTS chat_history;
DROP TABLE IF EXISTS emergency_events;
DROP TABLE IF EXISTS ai_recommendations;
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- Table: vehicle_types
-- ----------------------------------------------------------------------------
CREATE TABLE vehicle_types (
    type_name VARCHAR(20) PRIMARY KEY,
    description VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- Table: vehicles
-- ----------------------------------------------------------------------------
CREATE TABLE vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    owner_name VARCHAR(100) NULL,
    owner_contact VARCHAR(15) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_license_plate UNIQUE (license_plate),
    CONSTRAINT fk_vehicles_type FOREIGN KEY (vehicle_type) REFERENCES vehicle_types(type_name) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vehicles_plate ON vehicles(license_plate);
CREATE INDEX idx_vehicles_type ON vehicles(vehicle_type);

-- ----------------------------------------------------------------------------
-- Table: parking_slots
-- ----------------------------------------------------------------------------
CREATE TABLE parking_slots (
    id INT AUTO_INCREMENT PRIMARY KEY,
    slot_number VARCHAR(10) NOT NULL,
    zone_name VARCHAR(20) NOT NULL,
    slot_type VARCHAR(20) NOT NULL,
    is_occupied BOOLEAN DEFAULT FALSE,
    is_electric_charging BOOLEAN DEFAULT FALSE,
    distance_to_entrance INT DEFAULT 30,
    is_covered BOOLEAN DEFAULT FALSE,
    is_disabled_friendly BOOLEAN DEFAULT FALSE,
    is_premium BOOLEAN DEFAULT FALSE,
    is_emergency_reserved BOOLEAN DEFAULT FALSE,
    CONSTRAINT uk_slot_number UNIQUE (slot_number),
    CONSTRAINT fk_slots_type FOREIGN KEY (slot_type) REFERENCES vehicle_types(type_name) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_slots_search ON parking_slots(slot_number, zone_name);
CREATE INDEX idx_slots_availability ON parking_slots(slot_type, is_occupied);

-- ----------------------------------------------------------------------------
-- Table: parking_rates
-- ----------------------------------------------------------------------------
CREATE TABLE parking_rates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_type VARCHAR(20) NOT NULL,
    hourly_rate DECIMAL(10, 2) NOT NULL,
    grace_period_minutes INT DEFAULT 15,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_rate_vehicle_type UNIQUE (vehicle_type),
    CONSTRAINT fk_rates_type FOREIGN KEY (vehicle_type) REFERENCES vehicle_types(type_name) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- Table: parking_sessions
-- ----------------------------------------------------------------------------
CREATE TABLE parking_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT NOT NULL,
    slot_id INT NOT NULL,
    entry_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    exit_time TIMESTAMP NULL DEFAULT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, OVERDUE
    CONSTRAINT fk_sessions_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sessions_slot FOREIGN KEY (slot_id) REFERENCES parking_slots(id) ON DELETE RESTRICT,
    CONSTRAINT chk_session_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'OVERDUE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_sessions_active ON parking_sessions(status, entry_time);
CREATE INDEX idx_sessions_vehicle ON parking_sessions(vehicle_id);
CREATE INDEX idx_sessions_slot ON parking_sessions(slot_id);

-- ----------------------------------------------------------------------------
-- Table: payments
-- ----------------------------------------------------------------------------
CREATE TABLE payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(20) NOT NULL, -- CASH, CARD, UPI, MOBILE_WALLET
    payment_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PAID, FAILED, REFUNDED
    CONSTRAINT fk_payments_session FOREIGN KEY (session_id) REFERENCES parking_sessions(id) ON DELETE RESTRICT,
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CASH', 'CARD', 'UPI', 'MOBILE_WALLET')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payments_status ON payments(payment_status);
CREATE INDEX idx_payments_session ON payments(session_id);

-- ----------------------------------------------------------------------------
-- Table: pricing_rules
-- ----------------------------------------------------------------------------
CREATE TABLE pricing_rules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(50) NOT NULL,
    factor_type VARCHAR(30) NOT NULL,
    target_value VARCHAR(50) NOT NULL,
    multiplier DECIMAL(5,2) DEFAULT 1.00,
    flat_surcharge DECIMAL(10,2) DEFAULT 0.00,
    description VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- Table: weather_status
-- ----------------------------------------------------------------------------
CREATE TABLE weather_status (
    id INT PRIMARY KEY,
    current_weather VARCHAR(30) NOT NULL,
    temperature DOUBLE NOT NULL,
    weather_alert VARCHAR(255) NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- Table: chat_history
-- ----------------------------------------------------------------------------
CREATE TABLE chat_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    query_text TEXT NOT NULL,
    response_text TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- Table: emergency_events
-- ----------------------------------------------------------------------------
CREATE TABLE emergency_events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    allocated_slot VARCHAR(10) NOT NULL,
    arrival_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    priority_level VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- Table: ai_recommendations
-- ----------------------------------------------------------------------------
CREATE TABLE ai_recommendations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    slot_number VARCHAR(10) NOT NULL,
    recommended_for_plate VARCHAR(20) NULL,
    reason TEXT NOT NULL,
    recommended_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================================
-- Seed Reference Data
-- ============================================================================

-- Seed Vehicle Types
INSERT INTO vehicle_types (type_name, description) VALUES
('CAR', 'Standard passenger sedan, hatchback, SUV, or crossover'),
('BIKE', 'Motorcycles, scooters, and mopeds'),
('TRUCK', 'Heavy duty, commercial, or loading trucks'),
('EV', 'Electric vehicles with specialized charging compatibility'),
('AMBULANCE', 'Emergency Medical Service Ambulance'),
('POLICE', 'Police patrol vehicle'),
('FIRE_TRUCK', 'Fire emergency responder vehicle');

-- Seed Default Parking Rates
INSERT INTO parking_rates (vehicle_type, hourly_rate, grace_period_minutes) VALUES
('CAR', 30.00, 15),
('BIKE', 15.00, 10),
('TRUCK', 50.00, 20),
('EV', 25.00, 15),
('AMBULANCE', 0.00, 120),
('POLICE', 0.00, 120),
('FIRE_TRUCK', 0.00, 120);

-- Seed Dynamic Pricing Rules
INSERT INTO pricing_rules (rule_name, factor_type, target_value, multiplier, flat_surcharge, description) VALUES
('Weekend Surcharge', 'WEEKEND', 'TRUE', 1.20, 0.00, '20% surcharge on Saturdays and Sundays'),
('Peak Hour Surcharge', 'PEAK_HOUR', 'TRUE', 1.30, 0.00, '30% surcharge during high-congestion periods'),
('High Occupancy Surcharge', 'OCCUPANCY_HIGH', 'TRUE', 1.50, 0.00, '50% surcharge when parking is > 80% full'),
('Medium Occupancy Surcharge', 'OCCUPANCY_MED', 'TRUE', 1.20, 0.00, '20% surcharge when parking is > 50% full'),
('Premium Zone fee', 'PREMIUM_ZONE', 'TRUE', 1.00, 20.00, 'Flat surcharge of ₹20/hr for premium spots'),
('EV Charging fee', 'EV_CHARGING', 'TRUE', 1.00, 15.00, 'Flat surcharge of ₹15/hr for EV charging usage'),
('Covered Spot fee', 'COVERED_SPOT', 'TRUE', 1.00, 10.00, 'Flat surcharge of ₹10/hr for weather-protected spots'),
('Distance Convenience Surcharge', 'DISTANCE_NEAR', 'TRUE', 1.00, 5.00, 'Convenience fee for spots within 25m of entrance'),
('Distance Discount', 'DISTANCE_FAR', 'TRUE', 1.00, -5.00, 'Discount of ₹5/hr for walking distance spots (> 60m)');

-- Seed Initial Weather Status
INSERT INTO weather_status (id, current_weather, temperature, weather_alert) VALUES
(1, 'Sunny', 32.5, 'Normal Conditions');

-- Seed Sample Parking Slots matching Zones A (12), B (10), C (8)
INSERT INTO parking_slots (slot_number, zone_name, slot_type, is_occupied, is_electric_charging, distance_to_entrance, is_covered, is_disabled_friendly, is_premium, is_emergency_reserved) VALUES
-- Zone A: CAR & EV (12 slots)
('A1', 'Zone A', 'CAR', FALSE, FALSE, 12, TRUE, TRUE, TRUE, FALSE), -- Premium, Covered, Disabled access
('A2', 'Zone A', 'CAR', FALSE, FALSE, 18, TRUE, FALSE, TRUE, FALSE),
('A3', 'Zone A', 'CAR', FALSE, FALSE, 25, FALSE, FALSE, FALSE, FALSE),
('A4', 'Zone A', 'CAR', FALSE, FALSE, 32, FALSE, FALSE, FALSE, FALSE),
('A5', 'Zone A', 'CAR', FALSE, FALSE, 40, FALSE, FALSE, FALSE, FALSE),
('A6', 'Zone A', 'CAR', FALSE, FALSE, 48, FALSE, FALSE, FALSE, FALSE),
('A7', 'Zone A', 'CAR', FALSE, FALSE, 55, TRUE, FALSE, FALSE, FALSE),
('A8', 'Zone A', 'CAR', FALSE, FALSE, 65, TRUE, FALSE, FALSE, FALSE),
('A9', 'Zone A', 'EV',  FALSE, TRUE,  15, TRUE, FALSE, TRUE, FALSE),  -- Premium EV Spot
('A10', 'Zone A', 'EV', FALSE, TRUE,  20, TRUE, FALSE, TRUE, FALSE),
('A11', 'Zone A', 'EV', FALSE, TRUE,  35, FALSE, FALSE, FALSE, FALSE),
('A12', 'Zone A', 'EV', FALSE, TRUE,  50, FALSE, FALSE, FALSE, FALSE),

-- Zone B: BIKES (10 slots)
('B1', 'Zone B', 'BIKE', FALSE, FALSE, 15, TRUE, FALSE, FALSE, FALSE),
('B2', 'Zone B', 'BIKE', FALSE, FALSE, 22, TRUE, FALSE, FALSE, FALSE),
('B3', 'Zone B', 'BIKE', FALSE, FALSE, 28, FALSE, FALSE, FALSE, FALSE),
('B4', 'Zone B', 'BIKE', FALSE, FALSE, 35, FALSE, FALSE, FALSE, FALSE),
('B5', 'Zone B', 'BIKE', FALSE, FALSE, 42, FALSE, FALSE, FALSE, FALSE),
('B6', 'Zone B', 'BIKE', FALSE, FALSE, 50, FALSE, FALSE, FALSE, FALSE),
('B7', 'Zone B', 'BIKE', FALSE, FALSE, 58, TRUE, FALSE, FALSE, FALSE),
('B8', 'Zone B', 'BIKE', FALSE, FALSE, 66, TRUE, FALSE, FALSE, FALSE),
('B9', 'Zone B', 'BIKE', FALSE, FALSE, 14, TRUE, TRUE, TRUE, FALSE), -- Premium near entrance, disabled friendly
('B10', 'Zone B', 'BIKE', FALSE, FALSE, 20, TRUE, TRUE, TRUE, FALSE),

-- Zone C: TRUCKS & Emergency (8 slots)
('C1', 'Zone C', 'TRUCK', FALSE, FALSE, 30, FALSE, FALSE, FALSE, FALSE),
('C2', 'Zone C', 'TRUCK', FALSE, FALSE, 38, FALSE, FALSE, FALSE, FALSE),
('C3', 'Zone C', 'TRUCK', FALSE, FALSE, 46, FALSE, FALSE, FALSE, FALSE),
('C4', 'Zone C', 'TRUCK', FALSE, FALSE, 55, FALSE, FALSE, FALSE, FALSE),
('C5', 'Zone C', 'TRUCK', FALSE, FALSE, 64, TRUE, FALSE, FALSE, FALSE),
('C6', 'Zone C', 'TRUCK', FALSE, FALSE, 72, TRUE, FALSE, FALSE, FALSE),
('C7', 'Zone C', 'TRUCK', FALSE, FALSE, 10, TRUE, FALSE, TRUE, TRUE),  -- Emergency Reserved Slot
('C8', 'Zone C', 'TRUCK', FALSE, FALSE, 15, TRUE, FALSE, TRUE, TRUE);  -- Emergency Reserved Slot

