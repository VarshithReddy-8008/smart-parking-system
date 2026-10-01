package com.smartparking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_plate", nullable = false, length = 20)
    private String licensePlate;

    @Column(name = "vehicle_type", nullable = false, length = 20)
    private String vehicleType;

    @Column(name = "allocated_slot", nullable = false, length = 10)
    private String allocatedSlot;

    @Column(name = "arrival_time")
    @Builder.Default
    private LocalDateTime arrivalTime = LocalDateTime.now();

    @Column(name = "priority_level", nullable = false, length = 20)
    private String priorityLevel;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
