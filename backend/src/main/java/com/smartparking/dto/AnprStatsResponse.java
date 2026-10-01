package com.smartparking.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnprStatsResponse {
    private long todayScans;
    private long todaySuccessful;
    private double successRate;
    private double avgConfidence;
    private String lastScannedVehicle;
    private String lastScanTime;
}
