package com.smartparking.repository;

import com.smartparking.model.ParkingSlot;
import com.smartparking.model.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {
    Optional<ParkingSlot> findBySlotNumber(String slotNumber);
    List<ParkingSlot> findByIsOccupied(Boolean isOccupied);
    List<ParkingSlot> findBySlotTypeAndIsOccupied(VehicleType slotType, Boolean isOccupied);
    long countByIsOccupied(Boolean isOccupied);
    long countBySlotTypeAndIsOccupied(VehicleType slotType, Boolean isOccupied);
}
