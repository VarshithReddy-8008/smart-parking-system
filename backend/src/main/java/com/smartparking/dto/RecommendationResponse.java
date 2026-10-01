package com.smartparking.dto;

import com.smartparking.model.ParkingSlot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {
    private ParkingSlot recommendedSlot;
    private int score;
    private String reason; // Detailed bullet explanation
}
