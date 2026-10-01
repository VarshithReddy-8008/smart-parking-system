package com.smartparking.service;

import com.smartparking.dto.CheckOutResponse;
import com.smartparking.dto.OccupancyStats;
import com.smartparking.exception.ParkingException;
import com.smartparking.model.ParkingSession;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.ParkingRate;
import com.smartparking.model.Payment;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.ParkingRateRepository;
import com.smartparking.repository.PaymentRepository;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingServiceImpl implements ParkingService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingRateRepository parkingRateRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final PaymentRepository paymentRepository;
    private final RecommendationEngine recommendationEngine;
    private final PricingEngine pricingEngine;
    private final EmergencyService emergencyService;

    @Override
    public OccupancyStats getOccupancyStats() {
        List<ParkingSlot> slots = parkingSlotRepository.findAll();
        long total = slots.size();
        long occupied = slots.stream().filter(ParkingSlot::getIsOccupied).count();
        long available = total - occupied;

        Map<String, Long> availableByType = slots.stream()
                .filter(s -> !s.getIsOccupied())
                .collect(Collectors.groupingBy(s -> s.getSlotType().getTypeName(), Collectors.counting()));

        Map<String, Long> occupiedByType = slots.stream()
                .filter(ParkingSlot::getIsOccupied)
                .collect(Collectors.groupingBy(s -> s.getSlotType().getTypeName(), Collectors.counting()));

        // Ensure all vehicle types exist in stats, even with 0 counts
        List<VehicleType> types = vehicleTypeRepository.findAll();
        for (VehicleType type : types) {
            availableByType.putIfAbsent(type.getTypeName(), 0L);
            occupiedByType.putIfAbsent(type.getTypeName(), 0L);
        }

        return OccupancyStats.builder()
                .totalSlots(total)
                .occupiedSlots(occupied)
                .availableSlots(available)
                .availableByType(availableByType)
                .occupiedByType(occupiedByType)
                .build();
    }

    @Override
    @Transactional
    public ParkingSession checkIn(String licensePlate, String vehicleType, String ownerName, String ownerContact) {
        // Validate active session
        if (parkingSessionRepository.findByVehicleLicensePlateAndStatus(licensePlate, "ACTIVE").isPresent()) {
            throw new ParkingException("Vehicle with plate " + licensePlate + " is already checked in.");
        }

        // Validate vehicle type
        VehicleType vType = vehicleTypeRepository.findById(vehicleType)
                .orElseThrow(() -> new ParkingException("Unsupported vehicle type: " + vehicleType));

        // Find or create Vehicle record
        Vehicle vehicle = vehicleRepository.findByLicensePlate(licensePlate)
                .orElse(null);
        if (vehicle == null) {
            vehicle = Vehicle.builder()
                    .licensePlate(licensePlate)
                    .vehicleType(vType)
                    .ownerName(ownerName)
                    .ownerContact(ownerContact)
                    .build();
            vehicle = vehicleRepository.save(vehicle);
        } else {
            // Update owner information if it was modified
            if (ownerName != null && !ownerName.equals(vehicle.getOwnerName())) {
                vehicle.setOwnerName(ownerName);
            }
            if (ownerContact != null && !ownerContact.equals(vehicle.getOwnerContact())) {
                vehicle.setOwnerContact(ownerContact);
            }
            vehicle = vehicleRepository.save(vehicle);
        }

        // Find available slot using recommendation engine
        com.smartparking.dto.RecommendationResponse rec = recommendationEngine.recommendBestSlot(vehicleType, licensePlate, false, false, false);
        ParkingSlot slot = rec.getRecommendedSlot();
        if (slot == null) {
            throw new ParkingException("No available parking slots for vehicle type: " + vehicleType);
        }

        slot.setIsOccupied(true);
        parkingSlotRepository.save(slot);

        // If it is emergency type, trigger arrival
        if (isEmergency(vehicleType)) {
            emergencyService.triggerEmergencyArrival(licensePlate, vehicleType, slot.getSlotNumber());
        }

        // Record Parking Session
        ParkingSession session = ParkingSession.builder()
                .vehicle(vehicle)
                .parkingSlot(slot)
                .entryTime(LocalDateTime.now())
                .status("ACTIVE")
                .build();

        return parkingSessionRepository.save(session);
    }

    @Override
    @Transactional
    public CheckOutResponse checkOut(String licensePlate) {
        // Find the active session
        ParkingSession session = parkingSessionRepository.findByVehicleLicensePlateAndStatus(licensePlate, "ACTIVE")
                .orElseThrow(() -> new ParkingException("No active parking session found for vehicle: " + licensePlate));

        LocalDateTime exitTime = LocalDateTime.now();
        Duration duration = Duration.between(session.getEntryTime(), exitTime);
        long durationInMinutes = Math.max(1, duration.toMinutes());

        // Get Rate rules
        ParkingRate rate = parkingRateRepository.findByVehicleType(session.getVehicle().getVehicleType())
                .orElseThrow(() -> new ParkingException("Pricing rate not defined for type: " + session.getVehicle().getVehicleType().getTypeName()));

        // Calculate dynamic fee
        BigDecimal amount = BigDecimal.ZERO;
        long durationInSeconds = duration.getSeconds();
        long billableHours = (long) Math.ceil(durationInSeconds / 3600.0);
        if (billableHours == 0 && durationInSeconds > 0) {
            billableHours = 1; // 1 second to 60 minutes = 1 hour
        }

        if (billableHours > 0) {
            BigDecimal dynamicRate = pricingEngine.calculateRate(session.getVehicle().getVehicleType().getTypeName(), session.getParkingSlot());
            amount = dynamicRate.multiply(BigDecimal.valueOf(billableHours));
        }

        // If it is emergency type, resolve alert
        if (isEmergency(session.getVehicle().getVehicleType().getTypeName())) {
            emergencyService.resolveEmergency(licensePlate);
        }

        // Create Payment log
        Payment payment = Payment.builder()
                .parkingSession(session)
                .amount(amount)
                .paymentTime(LocalDateTime.now())
                .paymentMethod("CASH") // Default initial selection
                .paymentStatus(amount.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PENDING")
                .build();
        payment = paymentRepository.save(payment);

        // Free physical slot
        ParkingSlot slot = session.getParkingSlot();
        slot.setIsOccupied(false);
        parkingSlotRepository.save(slot);

        // Complete session
        session.setExitTime(exitTime);
        session.setStatus("COMPLETED");
        parkingSessionRepository.save(session);

        return CheckOutResponse.builder()
                .sessionId(session.getId())
                .licensePlate(licensePlate)
                .slotNumber(slot.getSlotNumber())
                .entryTime(session.getEntryTime())
                .exitTime(exitTime)
                .durationInMinutes(durationInMinutes)
                .amountDue(amount)
                .paymentId(payment.getId())
                .paymentStatus(payment.getPaymentStatus())
                .build();
    }

    @Override
    @Transactional
    public Payment processPayment(Long paymentId, String paymentMethod) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ParkingException("Payment record not found for ID: " + paymentId));

        if ("PAID".equals(payment.getPaymentStatus())) {
            return payment;
        }

        payment.setPaymentStatus("PAID");
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentTime(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Override
    public List<ParkingSlot> getAllSlots() {
        return parkingSlotRepository.findAll();
    }

    @Override
    public List<ParkingSession> getActiveSessions() {
        return parkingSessionRepository.findByStatus("ACTIVE");
    }

    @Override
    public java.util.Optional<Vehicle> getVehicleByLicensePlate(String licensePlate) {
        return vehicleRepository.findByLicensePlate(licensePlate);
    }

    private boolean isEmergency(String type) {
        return "AMBULANCE".equalsIgnoreCase(type) || "POLICE".equalsIgnoreCase(type) || "FIRE_TRUCK".equalsIgnoreCase(type);
    }
}
