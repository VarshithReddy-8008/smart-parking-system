package com.smartparking.controller;

import com.smartparking.dto.CheckInRequest;
import com.smartparking.dto.CheckOutResponse;
import com.smartparking.dto.OccupancyStats;
import com.smartparking.dto.PaymentRequest;
import com.smartparking.model.ParkingSession;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Payment;
import com.smartparking.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class ParkingController {

    private final ParkingService parkingService;

    @GetMapping("/slots")
    public ResponseEntity<List<ParkingSlot>> getAllSlots() {
        return ResponseEntity.ok(parkingService.getAllSlots());
    }

    @GetMapping("/stats")
    public ResponseEntity<OccupancyStats> getStats() {
        return ResponseEntity.ok(parkingService.getOccupancyStats());
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<List<ParkingSession>> getActiveSessions() {
        return ResponseEntity.ok(parkingService.getActiveSessions());
    }

    @PostMapping("/check-in")
    public ResponseEntity<ParkingSession> checkIn(@RequestBody CheckInRequest request) {
        ParkingSession session = parkingService.checkIn(
                request.getLicensePlate(),
                request.getVehicleType(),
                request.getOwnerName(),
                request.getOwnerContact()
        );
        return new ResponseEntity<>(session, HttpStatus.CREATED);
    }

    @PostMapping("/check-out")
    public ResponseEntity<CheckOutResponse> checkOut(@RequestParam String licensePlate) {
        CheckOutResponse response = parkingService.checkOut(licensePlate);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments/{paymentId}/pay")
    public ResponseEntity<Payment> pay(
            @PathVariable Long paymentId,
            @RequestBody PaymentRequest request
    ) {
        Payment payment = parkingService.processPayment(paymentId, request.getPaymentMethod());
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/vehicles/{licensePlate}")
    public ResponseEntity<com.smartparking.model.Vehicle> getVehicleDetails(@PathVariable String licensePlate) {
        return parkingService.getVehicleByLicensePlate(licensePlate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
