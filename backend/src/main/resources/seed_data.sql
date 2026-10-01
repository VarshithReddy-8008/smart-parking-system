-- Seed Reference Data for smart_parking_db
USE smart_parking_db;

-- Seed Vehicle Types
INSERT IGNORE INTO vehicle_types (type_name, description) VALUES
('CAR', 'Standard passenger sedan, hatchback, SUV, or crossover'),
('BIKE', 'Motorcycles, scooters, and mopeds'),
('TRUCK', 'Heavy duty, commercial, or loading trucks'),
('EV', 'Electric vehicles with specialized charging compatibility'),
('AMBULANCE', 'Emergency Medical Service Ambulance'),
('POLICE', 'Police patrol vehicle'),
('FIRE_TRUCK', 'Fire emergency responder vehicle');

-- Seed Default Parking Rates
INSERT IGNORE INTO parking_rates (vehicle_type, hourly_rate, grace_period_minutes, updated_at) VALUES
('CAR', 30.00, 15, NOW()),
('BIKE', 15.00, 10, NOW()),
('TRUCK', 50.00, 20, NOW()),
('EV', 25.00, 15, NOW()),
('AMBULANCE', 0.00, 120, NOW()),
('POLICE', 0.00, 120, NOW()),
('FIRE_TRUCK', 0.00, 120, NOW());

-- Seed Dynamic Pricing Rules
INSERT IGNORE INTO pricing_rules (rule_name, factor_type, target_value, multiplier, flat_surcharge, description) VALUES
('Weekend Surcharge', 'WEEKEND', 'TRUE', 1.20, 0.00, '20% surcharge on Saturdays and Sundays'),
('Peak Hour Surcharge', 'PEAK_HOUR', 'TRUE', 1.30, 0.00, '30% surcharge during high-congestion periods'),
('High Occupancy Surcharge', 'OCCUPANCY_HIGH', 'TRUE', 1.50, 0.00, '50% surcharge when parking is > 80% full'),
('Medium Occupancy Surcharge', 'OCCUPANCY_MED', 'TRUE', 1.20, 0.00, '20% surcharge when parking is > 50% full'),
('Premium Zone fee', 'PREMIUM_ZONE', 'TRUE', 1.00, 20.00, 'Flat surcharge of Rs20/hr for premium spots'),
('EV Charging fee', 'EV_CHARGING', 'TRUE', 1.00, 15.00, 'Flat surcharge of Rs15/hr for EV charging usage'),
('Covered Spot fee', 'COVERED_SPOT', 'TRUE', 1.00, 10.00, 'Flat surcharge of Rs10/hr for weather-protected spots'),
('Distance Convenience Surcharge', 'DISTANCE_NEAR', 'TRUE', 1.00, 5.00, 'Convenience fee for spots within 25m of entrance'),
('Distance Discount', 'DISTANCE_FAR', 'TRUE', 1.00, -5.00, 'Discount of Rs5/hr for walking distance spots (> 60m)');

-- Seed Initial Weather Status
INSERT IGNORE INTO weather_status (id, current_weather, temperature, weather_alert, last_updated) VALUES
(1, 'Sunny', 32.5, 'Normal Conditions', NOW());

-- Seed Sample Parking Slots: Zone A (12), Zone B (10), Zone C (8)
INSERT IGNORE INTO parking_slots (slot_number, zone_name, slot_type, is_occupied, is_electric_charging, distance_to_entrance, is_covered, is_disabled_friendly, is_premium, is_emergency_reserved) VALUES
-- Zone A: CAR & EV (12 slots)
('A1',  'Zone A', 'CAR',  FALSE, FALSE, 12, TRUE,  TRUE,  TRUE,  FALSE),
('A2',  'Zone A', 'CAR',  FALSE, FALSE, 18, TRUE,  FALSE, TRUE,  FALSE),
('A3',  'Zone A', 'CAR',  FALSE, FALSE, 25, FALSE, FALSE, FALSE, FALSE),
('A4',  'Zone A', 'CAR',  FALSE, FALSE, 32, FALSE, FALSE, FALSE, FALSE),
('A5',  'Zone A', 'CAR',  FALSE, FALSE, 40, FALSE, FALSE, FALSE, FALSE),
('A6',  'Zone A', 'CAR',  FALSE, FALSE, 48, FALSE, FALSE, FALSE, FALSE),
('A7',  'Zone A', 'CAR',  FALSE, FALSE, 55, TRUE,  FALSE, FALSE, FALSE),
('A8',  'Zone A', 'CAR',  FALSE, FALSE, 65, TRUE,  FALSE, FALSE, FALSE),
('A9',  'Zone A', 'EV',   FALSE, TRUE,  15, TRUE,  FALSE, TRUE,  FALSE),
('A10', 'Zone A', 'EV',   FALSE, TRUE,  20, TRUE,  FALSE, TRUE,  FALSE),
('A11', 'Zone A', 'EV',   FALSE, TRUE,  35, FALSE, FALSE, FALSE, FALSE),
('A12', 'Zone A', 'EV',   FALSE, TRUE,  50, FALSE, FALSE, FALSE, FALSE),
-- Zone B: BIKES (10 slots)
('B1',  'Zone B', 'BIKE', FALSE, FALSE, 15, TRUE,  FALSE, FALSE, FALSE),
('B2',  'Zone B', 'BIKE', FALSE, FALSE, 22, TRUE,  FALSE, FALSE, FALSE),
('B3',  'Zone B', 'BIKE', FALSE, FALSE, 28, FALSE, FALSE, FALSE, FALSE),
('B4',  'Zone B', 'BIKE', FALSE, FALSE, 35, FALSE, FALSE, FALSE, FALSE),
('B5',  'Zone B', 'BIKE', FALSE, FALSE, 42, FALSE, FALSE, FALSE, FALSE),
('B6',  'Zone B', 'BIKE', FALSE, FALSE, 50, FALSE, FALSE, FALSE, FALSE),
('B7',  'Zone B', 'BIKE', FALSE, FALSE, 58, TRUE,  FALSE, FALSE, FALSE),
('B8',  'Zone B', 'BIKE', FALSE, FALSE, 66, TRUE,  FALSE, FALSE, FALSE),
('B9',  'Zone B', 'BIKE', FALSE, FALSE, 14, TRUE,  TRUE,  TRUE,  FALSE),
('B10', 'Zone B', 'BIKE', FALSE, FALSE, 20, TRUE,  TRUE,  TRUE,  FALSE),
-- Zone C: TRUCKS & Emergency (8 slots)
('C1',  'Zone C', 'TRUCK', FALSE, FALSE, 30, FALSE, FALSE, FALSE, FALSE),
('C2',  'Zone C', 'TRUCK', FALSE, FALSE, 38, FALSE, FALSE, FALSE, FALSE),
('C3',  'Zone C', 'TRUCK', FALSE, FALSE, 46, FALSE, FALSE, FALSE, FALSE),
('C4',  'Zone C', 'TRUCK', FALSE, FALSE, 55, FALSE, FALSE, FALSE, FALSE),
('C5',  'Zone C', 'TRUCK', FALSE, FALSE, 64, TRUE,  FALSE, FALSE, FALSE),
('C6',  'Zone C', 'TRUCK', FALSE, FALSE, 72, TRUE,  FALSE, FALSE, FALSE),
('C7',  'Zone C', 'TRUCK', FALSE, FALSE, 10, TRUE,  FALSE, TRUE,  TRUE),
('C8',  'Zone C', 'TRUCK', FALSE, FALSE, 15, TRUE,  FALSE, TRUE,  TRUE);
