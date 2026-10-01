package com.smartparking.service;

import com.smartparking.dto.CheckOutResponse;
import com.smartparking.dto.OccupancyStats;
import com.smartparking.model.ParkingSession;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Payment;
import com.smartparking.model.Vehicle;
import java.util.List;
import java.util.Optional;

public interface ParkingService {
    OccupancyStats getOccupancyStats();
    ParkingSession checkIn(String licensePlate, String vehicleType, String ownerName, String ownerContact);
    CheckOutResponse checkOut(String licensePlate);
    Payment processPayment(Long paymentId, String paymentMethod);
    List<ParkingSlot> getAllSlots();
    List<ParkingSession> getActiveSessions();
    Optional<Vehicle> getVehicleByLicensePlate(String licensePlate);
}
