package com.smartparking.repository;

import com.smartparking.model.ParkingRate;
import com.smartparking.model.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ParkingRateRepository extends JpaRepository<ParkingRate, Long> {
    Optional<ParkingRate> findByVehicleType(VehicleType vehicleType);
}
