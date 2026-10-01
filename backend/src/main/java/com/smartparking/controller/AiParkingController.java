package com.smartparking.controller;

import com.smartparking.dto.ChatRequest;
import com.smartparking.dto.ChatResponse;
import com.smartparking.dto.PricingDetailsResponse;
import com.smartparking.dto.RecommendationResponse;
import com.smartparking.model.EmergencyEvent;
import com.smartparking.model.WeatherStatus;
import com.smartparking.service.ChatService;
import com.smartparking.service.EmergencyService;
import com.smartparking.service.PricingEngine;
import com.smartparking.service.RecommendationEngine;
import com.smartparking.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiParkingController {

    private final ChatService chatService;
    private final PricingEngine pricingEngine;
    private final WeatherService weatherService;
    private final EmergencyService emergencyService;
    private final RecommendationEngine recommendationEngine;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.processChatMessage(request.getMessage(), request.getLicensePlate());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pricing/current")
    public ResponseEntity<PricingDetailsResponse> getPricingDetails() {
        return ResponseEntity.ok(pricingEngine.getPricingDetails());
    }

    @GetMapping("/weather")
    public ResponseEntity<WeatherStatus> getWeather() {
        return ResponseEntity.ok(weatherService.getWeather());
    }

    @PostMapping("/weather/set")
    public ResponseEntity<WeatherStatus> setWeather(
            @RequestParam String weather,
            @RequestParam(required = false) Double temp,
            @RequestParam(required = false) String alert
    ) {
        WeatherStatus updated = weatherService.setWeather(weather, temp, alert);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/emergency/active")
    public ResponseEntity<List<EmergencyEvent>> getActiveEmergencyEvents() {
        return ResponseEntity.ok(emergencyService.getActiveEvents());
    }

    @GetMapping("/emergency/history")
    public ResponseEntity<List<EmergencyEvent>> getEmergencyHistory() {
        return ResponseEntity.ok(emergencyService.getEventHistory());
    }

    @GetMapping("/recommend")
    public ResponseEntity<RecommendationResponse> getRecommendation(
            @RequestParam(required = false, defaultValue = "CAR") String vehicleType,
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) Boolean needCovered,
            @RequestParam(required = false) Boolean needDisabled,
            @RequestParam(required = false) Boolean needEv
    ) {
        RecommendationResponse rec = recommendationEngine.recommendBestSlot(
                vehicleType,
                licensePlate,
                needCovered != null ? needCovered : false,
                needDisabled != null ? needDisabled : false,
                needEv != null ? needEv : false
        );
        return ResponseEntity.ok(rec);
    }
}
