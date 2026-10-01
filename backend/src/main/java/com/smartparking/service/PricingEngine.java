package com.smartparking.service;

import com.smartparking.dto.PricingDetailsResponse;
import com.smartparking.model.ParkingRate;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.PricingRule;
import com.smartparking.repository.ParkingRateRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PricingEngine {

    private final ParkingRateRepository parkingRateRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final PricingRuleRepository pricingRuleRepository;

    public BigDecimal calculateRate(String vehicleTypeName, ParkingSlot slot) {
        // 1. Get base rate for vehicle type
        ParkingRate rateInfo = parkingRateRepository.findAll().stream()
                .filter(r -> r.getVehicleType().getTypeName().equalsIgnoreCase(vehicleTypeName))
                .findFirst()
                .orElse(null);
        
        BigDecimal baseRate = rateInfo != null ? rateInfo.getHourlyRate() : BigDecimal.valueOf(30.00);

        // 2. Fetch current active rules
        List<PricingRule> rules = pricingRuleRepository.findAll();
        
        BigDecimal multiplier = BigDecimal.ONE;
        BigDecimal flatSurcharges = BigDecimal.ZERO;
        
        LocalDateTime now = LocalDateTime.now();

        // 3. Weekend check
        boolean isWeekend = isWeekend(now);
        if (isWeekend) {
            PricingRule weekendRule = rules.stream().filter(r -> r.getFactorType().equals("WEEKEND")).findFirst().orElse(null);
            if (weekendRule != null) {
                multiplier = multiplier.add(weekendRule.getMultiplier().subtract(BigDecimal.ONE));
            }
        }

        // 4. Peak hour check
        boolean isPeak = isPeakHour(now);
        if (isPeak) {
            PricingRule peakRule = rules.stream().filter(r -> r.getFactorType().equals("PEAK_HOUR")).findFirst().orElse(null);
            if (peakRule != null) {
                multiplier = multiplier.add(peakRule.getMultiplier().subtract(BigDecimal.ONE));
            }
        }

        // 5. Occupancy check
        double occupancy = getOccupancyPercentage();
        if (occupancy > 80) {
            PricingRule highOcc = rules.stream().filter(r -> r.getFactorType().equals("OCCUPANCY_HIGH")).findFirst().orElse(null);
            if (highOcc != null) {
                multiplier = multiplier.add(highOcc.getMultiplier().subtract(BigDecimal.ONE));
            }
        } else if (occupancy > 50) {
            PricingRule medOcc = rules.stream().filter(r -> r.getFactorType().equals("OCCUPANCY_MED")).findFirst().orElse(null);
            if (medOcc != null) {
                multiplier = multiplier.add(medOcc.getMultiplier().subtract(BigDecimal.ONE));
            }
        }

        // 6. Slot specific surcharges
        if (slot != null) {
            if (slot.getIsPremium()) {
                PricingRule prem = rules.stream().filter(r -> r.getFactorType().equals("PREMIUM_ZONE")).findFirst().orElse(null);
                if (prem != null) {
                    flatSurcharges = flatSurcharges.add(prem.getFlatSurcharge());
                }
            }
            if (slot.getIsElectricCharging()) {
                PricingRule ev = rules.stream().filter(r -> r.getFactorType().equals("EV_CHARGING")).findFirst().orElse(null);
                if (ev != null) {
                    flatSurcharges = flatSurcharges.add(ev.getFlatSurcharge());
                }
            }
            if (slot.getIsCovered()) {
                PricingRule covered = rules.stream().filter(r -> r.getFactorType().equals("COVERED_SPOT")).findFirst().orElse(null);
                if (covered != null) {
                    flatSurcharges = flatSurcharges.add(covered.getFlatSurcharge());
                }
            }
            if (slot.getDistanceToEntrance() < 25) {
                PricingRule near = rules.stream().filter(r -> r.getFactorType().equals("DISTANCE_NEAR")).findFirst().orElse(null);
                if (near != null) {
                    flatSurcharges = flatSurcharges.add(near.getFlatSurcharge());
                }
            } else if (slot.getDistanceToEntrance() > 60) {
                PricingRule far = rules.stream().filter(r -> r.getFactorType().equals("DISTANCE_FAR")).findFirst().orElse(null);
                if (far != null) {
                    flatSurcharges = flatSurcharges.add(far.getFlatSurcharge());
                }
            }
        }

        // Final rate: (BaseRate * Multiplier) + FlatSurcharges
        return baseRate.multiply(multiplier).add(flatSurcharges).setScale(2, RoundingMode.HALF_UP);
    }

    public PricingDetailsResponse getPricingDetails() {
        LocalDateTime now = LocalDateTime.now();
        double occupancy = getOccupancyPercentage();
        boolean weekend = isWeekend(now);
        boolean peak = isPeakHour(now);

        List<String> activeMultipliers = new ArrayList<>();
        if (weekend) activeMultipliers.add("Weekend (+20%)");
        if (peak) activeMultipliers.add("Peak Hour (+30%)");
        if (occupancy > 80) activeMultipliers.add("High Occupancy (+50%)");
        else if (occupancy > 50) activeMultipliers.add("Medium Occupancy (+20%)");

        Map<String, BigDecimal> rates = new HashMap<>();
        List<ParkingRate> baseRates = parkingRateRepository.findAll();
        for (ParkingRate base : baseRates) {
            BigDecimal currentRate = calculateRate(base.getVehicleType().getTypeName(), null);
            rates.put(base.getVehicleType().getTypeName(), currentRate);
        }

        // Trend calculation: dynamic based on occupancy
        String trend = "STABLE";
        if (occupancy > 70 || peak) {
            trend = "UP";
        } else if (occupancy < 30) {
            trend = "DOWN";
        }

        return PricingDetailsResponse.builder()
                .occupancyPercentage(occupancy)
                .isWeekend(weekend)
                .isPeakHour(peak)
                .priceTrend(trend)
                .currentRates(rates)
                .activeMultipliers(activeMultipliers)
                .build();
    }

    private boolean isWeekend(LocalDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private boolean isPeakHour(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        // Peak hours: 8:00-10:00, 12:00-14:00, 17:00-19:00
        return (time.isAfter(LocalTime.of(7, 59)) && time.isBefore(LocalTime.of(10, 1))) ||
               (time.isAfter(LocalTime.of(11, 59)) && time.isBefore(LocalTime.of(14, 1))) ||
               (time.isAfter(LocalTime.of(16, 59)) && time.isBefore(LocalTime.of(19, 1)));
    }

    private double getOccupancyPercentage() {
        long total = parkingSlotRepository.count();
        if (total == 0) return 0.0;
        long occupied = parkingSlotRepository.countByIsOccupied(true);
        return ((double) occupied / total) * 100.0;
    }
}
