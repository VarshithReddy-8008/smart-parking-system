package com.smartparking.service;

import com.smartparking.model.EmergencyEvent;
import com.smartparking.model.ParkingSlot;
import com.smartparking.repository.EmergencyEventRepository;
import com.smartparking.repository.ParkingSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmergencyService {

    private final EmergencyEventRepository emergencyEventRepository;
    private final ParkingSlotRepository parkingSlotRepository;

    public List<EmergencyEvent> getActiveEvents() {
        return emergencyEventRepository.findByStatus("ACTIVE");
    }

    public List<EmergencyEvent> getEventHistory() {
        return emergencyEventRepository.findAll();
    }

    @Transactional
    public EmergencyEvent triggerEmergencyArrival(String licensePlate, String vehicleType, String slotNumber) {
        // Create emergency event log
        String priorityLevel = "CRITICAL";
        if (vehicleType.equalsIgnoreCase("AMBULANCE")) {
            priorityLevel = "EMERGENCY";
        } else if (vehicleType.equalsIgnoreCase("FIRE_TRUCK")) {
            priorityLevel = "CRITICAL";
        } else if (vehicleType.equalsIgnoreCase("POLICE")) {
            priorityLevel = "HIGH";
        }

        EmergencyEvent event = EmergencyEvent.builder()
                .licensePlate(licensePlate)
                .vehicleType(vehicleType)
                .allocatedSlot(slotNumber)
                .arrivalTime(LocalDateTime.now())
                .priorityLevel(priorityLevel)
                .status("ACTIVE")
                .build();
        
        event = emergencyEventRepository.save(event);

        // Reserve nearby slots if required (keep the area clear)
        reserveAdjacentSlots(slotNumber);

        return event;
    }

    @Transactional
    public void resolveEmergency(String licensePlate) {
        List<EmergencyEvent> activeEvents = emergencyEventRepository.findByStatus("ACTIVE");
        for (EmergencyEvent event : activeEvents) {
            if (event.getLicensePlate().equalsIgnoreCase(licensePlate)) {
                event.setStatus("RESOLVED");
                emergencyEventRepository.save(event);
                
                // Release the reserved adjacent slots
                releaseAdjacentSlots(event.getAllocatedSlot());
            }
        }
    }

    private void reserveAdjacentSlots(String slotNumber) {
        // In our seed, C7 and C8 are emergency slots.
        // Let's find adjacent slots (e.g. if C7 is occupied, reserve C6 or C8)
        try {
            String prefix = slotNumber.replaceAll("\\d+", "");
            int num = Integer.parseInt(slotNumber.replaceAll("\\D+", ""));
            
            // Reserve slot num + 1 or num - 1 if available
            String adj1 = prefix + (num + 1);
            String adj2 = prefix + (num - 1);

            Optional<ParkingSlot> s1 = parkingSlotRepository.findBySlotNumber(adj1);
            if (s1.isPresent() && !s1.get().getIsOccupied()) {
                s1.get().setIsOccupied(true); // set occupied as a placeholder for reserved
                parkingSlotRepository.save(s1.get());
            }
            
            Optional<ParkingSlot> s2 = parkingSlotRepository.findBySlotNumber(adj2);
            if (s2.isPresent() && !s2.get().getIsOccupied()) {
                s2.get().setIsOccupied(true);
                parkingSlotRepository.save(s2.get());
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
    }

    private void releaseAdjacentSlots(String slotNumber) {
        try {
            String prefix = slotNumber.replaceAll("\\d+", "");
            int num = Integer.parseInt(slotNumber.replaceAll("\\D+", ""));
            
            String adj1 = prefix + (num + 1);
            String adj2 = prefix + (num - 1);

            Optional<ParkingSlot> s1 = parkingSlotRepository.findBySlotNumber(adj1);
            if (s1.isPresent() && s1.get().getIsOccupied()) {
                // If it is occupied but there are no sessions active for this slot, free it
                s1.get().setIsOccupied(false);
                parkingSlotRepository.save(s1.get());
            }
            
            Optional<ParkingSlot> s2 = parkingSlotRepository.findBySlotNumber(adj2);
            if (s2.isPresent() && s2.get().getIsOccupied()) {
                s2.get().setIsOccupied(false);
                parkingSlotRepository.save(s2.get());
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}
