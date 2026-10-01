package com.smartparking.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnprScanRequest {
    private String imageBase64; // Base64 encoded JPEG image from webcam or upload
    private String vehicleNumber; // Optional client-detected vehicle plate
    private Double confidence; // Optional client OCR confidence score
}
