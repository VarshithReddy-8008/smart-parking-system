package com.smartparking.service;

import com.smartparking.dto.ChatResponse;
import com.smartparking.dto.RecommendationResponse;
import com.smartparking.model.AiRecommendation;
import com.smartparking.model.ChatHistory;
import com.smartparking.model.ParkingSession;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.WeatherStatus;
import com.smartparking.repository.AiRecommendationRepository;
import com.smartparking.repository.ChatHistoryRepository;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.repository.ParkingSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatHistoryRepository chatHistoryRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final RecommendationEngine recommendationEngine;
    private final PricingEngine pricingEngine;
    private final WeatherService weatherService;

    public ChatResponse processChatMessage(String message, String licensePlate) {
        String msgLower = message.toLowerCase().trim();
        String reply = "";
        String recommendedSlot = null;

        if (msgLower.contains("nearest covered") || msgLower.contains("closest covered")) {
            RecommendationResponse rec = recommendationEngine.recommendBestSlot("CAR", licensePlate, true, false, false);
            if (rec.getRecommendedSlot() != null) {
                recommendedSlot = rec.getRecommendedSlot().getSlotNumber();
                reply = "I found the nearest covered parking for you:\n\n" + rec.getReason();
            } else {
                reply = "Sorry, all covered parking slots are currently occupied.";
            }
        } 
        else if (msgLower.contains("nearest") || msgLower.contains("closest") || msgLower.contains("entrance")) {
            // Find closest available slot to entrance
            List<ParkingSlot> slots = parkingSlotRepository.findByIsOccupied(false);
            Optional<ParkingSlot> closest = slots.stream().min(Comparator.comparingInt(ParkingSlot::getDistanceToEntrance));
            if (closest.isPresent()) {
                recommendedSlot = closest.get().getSlotNumber();
                reply = "The slot closest to the entrance is **" + recommendedSlot + "** (distance: " + closest.get().getDistanceToEntrance() + "m).\n\n" +
                        "Reason:\n" +
                        "• Super close walking distance\n" +
                        "• Highly accessible\n" +
                        "• Current price: ₹" + pricingEngine.calculateRate(closest.get().getSlotType().getTypeName(), closest.get()) + "/hr";
            } else {
                reply = "Sorry, no parking slots are currently available.";
            }
        }
        else if (msgLower.contains("elevator")) {
            // Slots in Zone A are closest to the elevator
            List<ParkingSlot> slots = parkingSlotRepository.findByIsOccupied(false).stream()
                    .filter(s -> s.getZoneName().equalsIgnoreCase("Zone A"))
                    .collect(Collectors.toList());
            Optional<ParkingSlot> closest = slots.stream().min(Comparator.comparingInt(ParkingSlot::getDistanceToEntrance));
            if (closest.isPresent()) {
                recommendedSlot = closest.get().getSlotNumber();
                reply = "Zone A slots are closest to the main building elevator. The nearest available one is **" + recommendedSlot + "** (" + closest.get().getDistanceToEntrance() + "m).\n\n" +
                        "Reason:\n" +
                        "• Direct elevator access\n" +
                        "• Weather-protected zone\n" +
                        "• Current price: ₹" + pricingEngine.calculateRate(closest.get().getSlotType().getTypeName(), closest.get()) + "/hr";
            } else {
                reply = "Sorry, all slots near the elevator (Zone A) are currently occupied.";
            }
        }
        else if (msgLower.contains("cheapest") || msgLower.contains("lowest price") || msgLower.contains("low cost") || msgLower.contains("budget")) {
            List<ParkingSlot> slots = parkingSlotRepository.findByIsOccupied(false);
            ParkingSlot cheapestSlot = null;
            BigDecimal cheapestRate = BigDecimal.valueOf(99999);

            for (ParkingSlot s : slots) {
                BigDecimal rate = pricingEngine.calculateRate(s.getSlotType().getTypeName(), s);
                if (rate.compareTo(cheapestRate) < 0) {
                    cheapestRate = rate;
                    cheapestSlot = s;
                }
            }

            if (cheapestSlot != null) {
                recommendedSlot = cheapestSlot.getSlotNumber();
                reply = "The cheapest parking slot currently is **" + recommendedSlot + "** at **₹" + cheapestRate + "/hour**.\n\n" +
                        "Reason:\n" +
                        "• Off-peak zone discount\n" +
                        "• Walking distance: " + cheapestSlot.getDistanceToEntrance() + " meters\n" +
                        "• Saves up to 40% compared to premium slots";
            } else {
                reply = "Sorry, no parking slots are currently available.";
            }
        }
        else if (msgLower.contains("available") || msgLower.contains("availability") || msgLower.contains("free spots")) {
            List<ParkingSlot> slots = parkingSlotRepository.findAll();
            long total = slots.size();
            long free = slots.stream().filter(s -> !s.getIsOccupied()).count();
            long carFree = slots.stream().filter(s -> !s.getIsOccupied() && s.getSlotType().getTypeName().equals("CAR")).count();
            long bikeFree = slots.stream().filter(s -> !s.getIsOccupied() && s.getSlotType().getTypeName().equals("BIKE")).count();
            long evFree = slots.stream().filter(s -> !s.getIsOccupied() && s.getSlotType().getTypeName().equals("EV")).count();

            reply = "Here is the current parking availability status:\n\n" +
                    "• **Total Available:** " + free + " / " + total + " slots\n" +
                    "• **Standard Cars:** " + carFree + " slots free\n" +
                    "• **Motorcycles/Bikes:** " + bikeFree + " slots free\n" +
                    "• **EV Charging:** " + evFree + " slots free\n\n" +
                    "Would you like me to recommend a slot for you?";
        }
        else if (msgLower.contains("cost for") || msgLower.contains("price for") || msgLower.contains("how much")) {
            int hours = 3;
            Pattern p = Pattern.compile("(\\d+)\\s*hour");
            Matcher m = p.matcher(msgLower);
            if (m.find()) {
                hours = Integer.parseInt(m.group(1));
            }
            
            BigDecimal hourlyRate = pricingEngine.calculateRate("CAR", null);
            BigDecimal totalCost = hourlyRate.multiply(BigDecimal.valueOf(hours));
            reply = "Based on current dynamic rates (₹" + hourlyRate + "/hour for Cars):\n\n" +
                    "• **Duration:** " + hours + " hours\n" +
                    "• **Total Estimated Cost:** **₹" + totalCost + "**\n\n" +
                    "Note: Surcharges (Covered +₹10, Premium +₹20) or discounts may apply depending on the specific slot allocated.";
        }
        else if (msgLower.contains("where did i park yesterday") || msgLower.contains("where did i park") || msgLower.contains("find my vehicle") || msgLower.contains("find my car")) {
            if (licensePlate == null || licensePlate.isEmpty()) {
                // Try to extract license plate from message (e.g. MH-12-AB-1234)
                Pattern platePattern = Pattern.compile("([A-Z]{2}[-\\s]?\\d{2}[-\\s]?[A-Z]{1,2}[-\\s]?\\d{4})");
                Matcher pm = platePattern.matcher(message.toUpperCase());
                if (pm.find()) {
                    licensePlate = pm.group(1);
                }
            }

            if (licensePlate != null && !licensePlate.isEmpty()) {
                final String finalPlate = licensePlate;
                // Check active session first
                Optional<ParkingSession> activeSes = parkingSessionRepository.findByVehicleLicensePlateAndStatus(finalPlate, "ACTIVE");
                if (activeSes.isPresent()) {
                    recommendedSlot = activeSes.get().getParkingSlot().getSlotNumber();
                    reply = "Your vehicle **" + finalPlate + "** is currently parked in slot **" + recommendedSlot + "** (Zone " + activeSes.get().getParkingSlot().getZoneName() + ").";
                } else {
                    // Check completed sessions
                    List<ParkingSession> history = parkingSessionRepository.findAll().stream()
                            .filter(s -> s.getVehicle().getLicensePlate().equalsIgnoreCase(finalPlate))
                            .sorted(Comparator.comparing(ParkingSession::getEntryTime).reversed())
                            .collect(Collectors.toList());
                    if (!history.isEmpty()) {
                        ParkingSession lastSes = history.get(0);
                        reply = "Your last recorded session for **" + finalPlate + "** was in slot **" + lastSes.getParkingSlot().getSlotNumber() + "** on " + lastSes.getEntryTime().toLocalDate() + ".";
                    } else {
                        reply = "I couldn't find any parking session records for plate **" + finalPlate + "**.";
                    }
                }
            } else {
                reply = "Please specify your vehicle's license plate number (e.g. 'Where did I park MH-12-AB-1234?').";
            }
        }
        else if (msgLower.contains("ev charging") || msgLower.contains("ev charger") || msgLower.contains("ev slot")) {
            RecommendationResponse rec = recommendationEngine.recommendBestSlot("EV", licensePlate, false, false, true);
            if (rec.getRecommendedSlot() != null) {
                recommendedSlot = rec.getRecommendedSlot().getSlotNumber();
                reply = "Here is the best available EV charging slot:\n\n" + rec.getReason();
            } else {
                reply = "Sorry, all EV charging slots are currently occupied.";
            }
        }
        else if (msgLower.contains("disabled") || msgLower.contains("handicap") || msgLower.contains("accessible")) {
            RecommendationResponse rec = recommendationEngine.recommendBestSlot("CAR", licensePlate, false, true, false);
            if (rec.getRecommendedSlot() != null) {
                recommendedSlot = rec.getRecommendedSlot().getSlotNumber();
                reply = "I've located a disabled-friendly slot for you:\n\n" + rec.getReason();
            } else {
                reply = "Sorry, all disabled-friendly slots are currently occupied.";
            }
        }
        else if (msgLower.contains("weather")) {
            WeatherStatus ws = weatherService.getWeather();
            reply = "Current Weather: **" + ws.getCurrentWeather() + "** (" + ws.getTemperature() + "°C)\n" +
                    "Alerts: " + ws.getWeatherAlert() + "\n\n" +
                    "AI Recommendation: " + weatherService.getRecommendationAdvice(ws.getCurrentWeather());
        }
        else {
            // General recommendation query
            RecommendationResponse rec = recommendationEngine.recommendBestSlot("CAR", licensePlate, false, false, false);
            if (rec.getRecommendedSlot() != null) {
                recommendedSlot = rec.getRecommendedSlot().getSlotNumber();
                reply = "Hello! I am your AI Parking Assistant. Here is my recommendation for you:\n\n" + rec.getReason();
            } else {
                reply = "Welcome to ParkSmart! How can I help you today? You can ask me to find nearest parking, cheapest slots, EV slots, weather status, or locate your parked car.";
            }
        }

        // 7. Save history
        ChatHistory history = ChatHistory.builder()
                .queryText(message)
                .responseText(reply)
                .timestamp(LocalDateTime.now())
                .build();
        chatHistoryRepository.save(history);

        // 8. Log AI recommendations
        if (recommendedSlot != null) {
            AiRecommendation recLog = AiRecommendation.builder()
                    .slotNumber(recommendedSlot)
                    .recommendedForPlate(licensePlate)
                    .reason(reply)
                    .recommendedAt(LocalDateTime.now())
                    .build();
            aiRecommendationRepository.save(recLog);
        }

        return ChatResponse.builder()
                .reply(reply)
                .recommendedSlot(recommendedSlot)
                .build();
    }
}
