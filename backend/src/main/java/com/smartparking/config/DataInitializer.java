package com.smartparking.config;

import com.smartparking.model.ParkingRate;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.PricingRule;
import com.smartparking.model.VehicleType;
import com.smartparking.model.WeatherStatus;
import com.smartparking.repository.ParkingRateRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.PricingRuleRepository;
import com.smartparking.repository.VehicleTypeRepository;
import com.smartparking.repository.WeatherStatusRepository;
import com.smartparking.model.ParkingSession;
import com.smartparking.repository.ParkingSessionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final ParkingRateRepository parkingRateRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final WeatherStatusRepository weatherStatusRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingSessionRepository parkingSessionRepository;

    @Override
    public void run(String... args) throws Exception {
        // Fix any active sessions that were recorded under UTC timezone
        try {
            List<ParkingSession> activeSessions = parkingSessionRepository.findByStatus("ACTIVE");
            for (ParkingSession s : activeSessions) {
                if (s.getEntryTime() != null && s.getEntryTime().getHour() < 13) {
                    LocalDateTime corrected = s.getEntryTime().plusHours(5).plusMinutes(30);
                    log.info("Correcting entryTime for session ID {}: {} -> {}", s.getId(), s.getEntryTime(), corrected);
                    s.setEntryTime(corrected);
                    parkingSessionRepository.save(s);
                }
            }
        } catch (Exception e) {
            log.warn("Could not adjust active session times: {}", e.getMessage());
        }

        if (parkingSlotRepository.count() > 0) {
            log.info("Database already initialized with {} parking slots. Skipping seed.", parkingSlotRepository.count());
            return;
        }

        log.info("Empty database detected. Starting automated initial data seeding...");

        // 1. Vehicle Types
        Map<String, VehicleType> typeMap = new HashMap<>();
        String[][] types = {
            {"CAR", "Standard passenger sedan, hatchback, SUV, or crossover"},
            {"BIKE", "Motorcycles, scooters, and mopeds"},
            {"TRUCK", "Heavy duty, commercial, or loading trucks"},
            {"EV", "Electric vehicles with specialized charging compatibility"},
            {"AMBULANCE", "Emergency Medical Service Ambulance"},
            {"POLICE", "Police patrol vehicle"},
            {"FIRE_TRUCK", "Fire emergency responder vehicle"}
        };

        for (String[] t : types) {
            VehicleType vt = vehicleTypeRepository.findById(t[0])
                .orElseGet(() -> vehicleTypeRepository.save(
                    VehicleType.builder().typeName(t[0]).description(t[1]).build()
                ));
            typeMap.put(t[0], vt);
        }
        log.info("Seeded {} vehicle types.", typeMap.size());

        // 2. Parking Rates
        if (parkingRateRepository.count() == 0) {
            Object[][] rates = {
                {"CAR", "30.00", 15},
                {"BIKE", "15.00", 10},
                {"TRUCK", "50.00", 20},
                {"EV", "25.00", 15},
                {"AMBULANCE", "0.00", 120},
                {"POLICE", "0.00", 120},
                {"FIRE_TRUCK", "0.00", 120}
            };
            for (Object[] r : rates) {
                VehicleType vt = typeMap.get((String) r[0]);
                if (vt != null) {
                    parkingRateRepository.save(ParkingRate.builder()
                        .vehicleType(vt)
                        .hourlyRate(new BigDecimal((String) r[1]))
                        .gracePeriodMinutes((Integer) r[2])
                        .updatedAt(LocalDateTime.now())
                        .build());
                }
            }
            log.info("Seeded default parking rates.");
        }

        // 3. Dynamic Pricing Rules
        if (pricingRuleRepository.count() == 0) {
            pricingRuleRepository.saveAll(Arrays.asList(
                PricingRule.builder().ruleName("Weekend Surcharge").factorType("WEEKEND").targetValue("TRUE").multiplier(new BigDecimal("1.20")).flatSurcharge(BigDecimal.ZERO).description("20% surcharge on Saturdays and Sundays").build(),
                PricingRule.builder().ruleName("Peak Hour Surcharge").factorType("PEAK_HOUR").targetValue("TRUE").multiplier(new BigDecimal("1.30")).flatSurcharge(BigDecimal.ZERO).description("30% surcharge during high-congestion periods").build(),
                PricingRule.builder().ruleName("High Occupancy Surcharge").factorType("OCCUPANCY_HIGH").targetValue("TRUE").multiplier(new BigDecimal("1.50")).flatSurcharge(BigDecimal.ZERO).description("50% surcharge when parking is > 80% full").build(),
                PricingRule.builder().ruleName("Medium Occupancy Surcharge").factorType("OCCUPANCY_MED").targetValue("TRUE").multiplier(new BigDecimal("1.20")).flatSurcharge(BigDecimal.ZERO).description("20% surcharge when parking is > 50% full").build(),
                PricingRule.builder().ruleName("Premium Zone fee").factorType("PREMIUM_ZONE").targetValue("TRUE").multiplier(BigDecimal.ONE).flatSurcharge(new BigDecimal("20.00")).description("Flat surcharge of Rs20/hr for premium spots").build(),
                PricingRule.builder().ruleName("EV Charging fee").factorType("EV_CHARGING").targetValue("TRUE").multiplier(BigDecimal.ONE).flatSurcharge(new BigDecimal("15.00")).description("Flat surcharge of Rs15/hr for EV charging usage").build(),
                PricingRule.builder().ruleName("Covered Spot fee").factorType("COVERED_SPOT").targetValue("TRUE").multiplier(BigDecimal.ONE).flatSurcharge(new BigDecimal("10.00")).description("Flat surcharge of Rs10/hr for weather-protected spots").build(),
                PricingRule.builder().ruleName("Distance Convenience Surcharge").factorType("DISTANCE_NEAR").targetValue("TRUE").multiplier(BigDecimal.ONE).flatSurcharge(new BigDecimal("5.00")).description("Convenience fee for spots within 25m of entrance").build(),
                PricingRule.builder().ruleName("Distance Discount").factorType("DISTANCE_FAR").targetValue("TRUE").multiplier(BigDecimal.ONE).flatSurcharge(new BigDecimal("-5.00")).description("Discount of Rs5/hr for walking distance spots (> 60m)").build()
            ));
            log.info("Seeded dynamic pricing rules.");
        }

        // 4. Weather Status
        if (weatherStatusRepository.count() == 0) {
            weatherStatusRepository.save(WeatherStatus.builder()
                .id(1)
                .currentWeather("Sunny")
                .temperature(32.5)
                .weatherAlert("Normal Conditions")
                .lastUpdated(LocalDateTime.now())
                .build());
            log.info("Seeded initial weather status.");
        }

        // 5. Parking Slots (Zone A: 12, Zone B: 10, Zone C: 8)
        VehicleType carType = typeMap.get("CAR");
        VehicleType evType = typeMap.get("EV");
        VehicleType bikeType = typeMap.get("BIKE");
        VehicleType truckType = typeMap.get("TRUCK");

        // Zone A: CAR & EV (12 slots)
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("A1").zoneName("Zone A").slotType(carType).isOccupied(false).isElectricCharging(false).distanceToEntrance(12).isCovered(true).isDisabledFriendly(true).isPremium(true).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("A2").zoneName("Zone A").slotType(carType).isOccupied(false).isElectricCharging(false).distanceToEntrance(18).isCovered(true).isDisabledFriendly(false).isPremium(true).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("A3").zoneName("Zone A").slotType(carType).isOccupied(false).isElectricCharging(false).distanceToEntrance(25).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("A4").zoneName("Zone A").slotType(carType).isOccupied(false).isElectricCharging(false).distanceToEntrance(32).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("A5").zoneName("Zone A").slotType(carType).isOccupied(false).isElectricCharging(false).distanceToEntrance(40).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("A6").zoneName("Zone A").slotType(carType).isOccupied(false).isElectricCharging(false).distanceToEntrance(48).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("A7").zoneName("Zone A").slotType(carType).isOccupied(false).isElectricCharging(false).distanceToEntrance(55).isCovered(true).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("A8").zoneName("Zone A").slotType(carType).isOccupied(false).isElectricCharging(false).distanceToEntrance(65).isCovered(true).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("A9").zoneName("Zone A").slotType(evType).isOccupied(false).isElectricCharging(true).distanceToEntrance(15).isCovered(true).isDisabledFriendly(false).isPremium(true).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("A10").zoneName("Zone A").slotType(evType).isOccupied(false).isElectricCharging(true).distanceToEntrance(20).isCovered(true).isDisabledFriendly(false).isPremium(true).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("A11").zoneName("Zone A").slotType(evType).isOccupied(false).isElectricCharging(true).distanceToEntrance(35).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("A12").zoneName("Zone A").slotType(evType).isOccupied(false).isElectricCharging(true).distanceToEntrance(50).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());

        // Zone B: BIKES (10 slots)
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("B1").zoneName("Zone B").slotType(bikeType).isOccupied(false).isElectricCharging(false).distanceToEntrance(15).isCovered(true).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("B2").zoneName("Zone B").slotType(bikeType).isOccupied(false).isElectricCharging(false).distanceToEntrance(22).isCovered(true).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("B3").zoneName("Zone B").slotType(bikeType).isOccupied(false).isElectricCharging(false).distanceToEntrance(28).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("B4").zoneName("Zone B").slotType(bikeType).isOccupied(false).isElectricCharging(false).distanceToEntrance(35).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("B5").zoneName("Zone B").slotType(bikeType).isOccupied(false).isElectricCharging(false).distanceToEntrance(42).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("B6").zoneName("Zone B").slotType(bikeType).isOccupied(false).isElectricCharging(false).distanceToEntrance(50).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("B7").zoneName("Zone B").slotType(bikeType).isOccupied(false).isElectricCharging(false).distanceToEntrance(58).isCovered(true).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("B8").zoneName("Zone B").slotType(bikeType).isOccupied(false).isElectricCharging(false).distanceToEntrance(66).isCovered(true).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("B9").zoneName("Zone B").slotType(bikeType).isOccupied(false).isElectricCharging(false).distanceToEntrance(14).isCovered(true).isDisabledFriendly(true).isPremium(true).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("B10").zoneName("Zone B").slotType(bikeType).isOccupied(false).isElectricCharging(false).distanceToEntrance(20).isCovered(true).isDisabledFriendly(true).isPremium(true).isEmergencyReserved(false).build());

        // Zone C: TRUCKS & Emergency (8 slots)
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("C1").zoneName("Zone C").slotType(truckType).isOccupied(false).isElectricCharging(false).distanceToEntrance(30).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("C2").zoneName("Zone C").slotType(truckType).isOccupied(false).isElectricCharging(false).distanceToEntrance(38).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("C3").zoneName("Zone C").slotType(truckType).isOccupied(false).isElectricCharging(false).distanceToEntrance(46).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("C4").zoneName("Zone C").slotType(truckType).isOccupied(false).isElectricCharging(false).distanceToEntrance(55).isCovered(false).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("C5").zoneName("Zone C").slotType(truckType).isOccupied(false).isElectricCharging(false).distanceToEntrance(64).isCovered(true).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("C6").zoneName("Zone C").slotType(truckType).isOccupied(false).isElectricCharging(false).distanceToEntrance(72).isCovered(true).isDisabledFriendly(false).isPremium(false).isEmergencyReserved(false).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("C7").zoneName("Zone C").slotType(truckType).isOccupied(false).isElectricCharging(false).distanceToEntrance(10).isCovered(true).isDisabledFriendly(false).isPremium(true).isEmergencyReserved(true).build());
        parkingSlotRepository.save(ParkingSlot.builder().slotNumber("C8").zoneName("Zone C").slotType(truckType).isOccupied(false).isElectricCharging(false).distanceToEntrance(15).isCovered(true).isDisabledFriendly(false).isPremium(true).isEmergencyReserved(true).build());

        log.info("Successfully seeded all 30 parking slots across Zones A, B, and C.");
    }
}
