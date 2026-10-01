package com.smartparking.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnprScanRequest {
    private String imageBase64; // Base64 encoded JPEG image from webcam
}
