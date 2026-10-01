package com.smartparking.service;

import com.smartparking.dto.RecommendationResponse;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.WeatherStatus;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.WeatherStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationEngine {

    private final ParkingSlotRepository parkingSlotRepository;
    private final WeatherService weatherService;

    public RecommendationResponse recommendBestSlot(String vehicleType, String licensePlate, Boolean needCovered, Boolean needDisabled, Boolean needEv) {
        List<ParkingSlot> availableSlots = parkingSlotRepository.findByIsOccupied(false);
        if (availableSlots.isEmpty()) {
            return RecommendationResponse.builder()
                    .recommendedSlot(null)
                    .score(0)
                    .reason("No available slots in the entire parking lot.")
                    .build();
        }

        WeatherStatus weather = weatherService.getWeather();
        String currentWeather = weather.getCurrentWeather();

        ParkingSlot bestSlot = null;
        int bestScore = -9999;
        String bestReason = "";

        // Iterate through all slots to calculate score
        for (ParkingSlot slot : availableSlots) {
            int score = 100;
            List<String> reasons = new ArrayList<>();

            // 1. Vehicle category compatibility check
            // If the vehicle type is emergency, it must go to an emergency reserved slot if possible.
            boolean isEmergencyType = isEmergencyVehicle(vehicleType);
            
            if (isEmergencyType) {
                if (slot.getIsEmergencyReserved()) {
                    score += 1000;
                    reasons.add("Designated Emergency Reserved spot (+1000)");
                } else {
                    score -= 100;
                    reasons.add("Non-emergency slot penalty for emergency vehicle (-100)");
                }
            } else {
                if (slot.getIsEmergencyReserved()) {
                    score -= 5000; // Strictly exclude emergency slots for standard vehicles
                    reasons.add("Emergency reserved only");
                }
            }

            // Type mismatch check
            if (!isEmergencyType) {
                if (vehicleType != null && !slot.getSlotType().getTypeName().equalsIgnoreCase(vehicleType)) {
                    // Soft penalty or hard filter? Let's filter out completely unless it is a generic fit
                    score -= 2000;
                    reasons.add("Category mismatch");
                }
            }

            // 2. Distance to Entrance scoring (closer is better)
            int distScore = (int) Math.round(slot.getDistanceToEntrance() * 0.5);
            score -= distScore;
            reasons.add("Distance: " + slot.getDistanceToEntrance() + " meters (-" + distScore + ")");

            // 3. Weather aware recommendation
            boolean weatherCoveredNeed = currentWeather.equalsIgnoreCase("Rain") || 
                                         currentWeather.equalsIgnoreCase("Storm") || 
                                         currentWeather.equalsIgnoreCase("Extreme Heat");
            
            if (slot.getIsCovered()) {
                if (weatherCoveredNeed || (needCovered != null && needCovered)) {
                    score += 50;
                    reasons.add("Covered spot matches weather needs / preferences (+50)");
                } else {
                    score += 15;
                    reasons.add("Covered spot convenience (+15)");
                }
            } else {
                if (weatherCoveredNeed) {
                    score -= 30;
                    reasons.add("Uncovered spot exposed to bad weather (" + currentWeather + ") (-30)");
                }
            }

            // 4. EV Charger matching
            if (slot.getIsElectricCharging()) {
                if (vehicleType != null && vehicleType.equalsIgnoreCase("EV")) {
                    score += 80;
                    reasons.add("EV Charger available for EV vehicle (+80)");
                } else if (needEv != null && needEv) {
                    score += 80;
                    reasons.add("EV Charger requested (+80)");
                } else {
                    // Occupying EV chargers by standard cars is penalized
                    score -= 60;
                    reasons.add("Penalized EV slot for non-EV vehicle (-60)");
                }
            } else {
                if (vehicleType != null && vehicleType.equalsIgnoreCase("EV")) {
                    score -= 40;
                    reasons.add("EV vehicle lacks charging charger (-40)");
                }
            }

            // 5. Disabled access matching
            if (slot.getIsDisabledFriendly()) {
                if (needDisabled != null && needDisabled) {
                    score += 100;
                    reasons.add("Disabled accessible slot matches preference (+100)");
                } else {
                    // Penalize standard vehicles occupying disabled slots
                    score -= 50;
                    reasons.add("Reserved for disabled friendly drivers (-50)");
                }
            } else {
                if (needDisabled != null && needDisabled) {
                    score -= 50;
                    reasons.add("Lacks wheelchair access (-50)");
                }
            }

            // 6. Premium zones
            if (slot.getIsPremium()) {
                score += 20;
                reasons.add("Premium zone spot (+20)");
            }

            // Compare and set
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
                
                StringBuilder sb = new StringBuilder();
                sb.append("Recommended Slot: ").append(slot.getSlotNumber()).append("\n\nReason:\n");
                for (String r : reasons) {
                    sb.append("• ").append(r).append("\n");
                }
                bestReason = sb.toString();
            }
        }

        return RecommendationResponse.builder()
                .recommendedSlot(bestSlot)
                .score(bestScore)
                .reason(bestReason)
                .build();
    }

    private boolean isEmergencyVehicle(String type) {
        if (type == null) return false;
        return type.equalsIgnoreCase("AMBULANCE") || 
               type.equalsIgnoreCase("POLICE") || 
               type.equalsIgnoreCase("FIRE_TRUCK");
    }
}
