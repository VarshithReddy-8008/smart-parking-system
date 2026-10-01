package com.smartparking.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "number_plate_scans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NumberPlateScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "image_path", columnDefinition = "TEXT")
    private String imagePath;

    @Column(name = "scan_time")
    @Builder.Default
    private LocalDateTime scanTime = LocalDateTime.now();

    @Column(name = "scan_success")
    private Boolean scanSuccess;

    @Column(name = "confirmed_by_user")
    private Boolean confirmedByUser;

    @Column(name = "final_vehicle_number", length = 20)
    private String finalVehicleNumber;
}
