package com.smartparking.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnprScanResponse {
    private Long scanId;
    private boolean success;
    private String vehicleNumber;
    private double confidence;
    private String message;
    private String imagePath;
}
