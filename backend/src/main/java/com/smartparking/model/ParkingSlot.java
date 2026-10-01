package com.smartparking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slot_number", nullable = false, unique = true, length = 10)
    private String slotNumber;

    @Column(name = "zone_name", nullable = false, length = 20)
    private String zoneName;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "slot_type", referencedColumnName = "type_name", nullable = false)
    private VehicleType slotType;

    @Column(name = "is_occupied", nullable = false)
    @Builder.Default
    private Boolean isOccupied = false;

    @Column(name = "is_electric_charging", nullable = false)
    @Builder.Default
    private Boolean isElectricCharging = false;

    @Column(name = "distance_to_entrance", nullable = false)
    @Builder.Default
    private Integer distanceToEntrance = 30;

    @Column(name = "is_covered", nullable = false)
    @Builder.Default
    private Boolean isCovered = false;

    @Column(name = "is_disabled_friendly", nullable = false)
    @Builder.Default
    private Boolean isDisabledFriendly = false;

    @Column(name = "is_premium", nullable = false)
    @Builder.Default
    private Boolean isPremium = false;

    @Column(name = "is_emergency_reserved", nullable = false)
    @Builder.Default
    private Boolean isEmergencyReserved = false;
}
