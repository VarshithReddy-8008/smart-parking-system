package com.smartparking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingDetailsResponse {
    private double occupancyPercentage;
    private boolean isWeekend;
    private boolean isPeakHour;
    private String priceTrend; // STABLE, UP, DOWN
    private Map<String, BigDecimal> currentRates; // Vehicle Type -> Current Rate
    private List<String> activeMultipliers; // e.g. ["Weekend (+20%)", "Occupancy High (+50%)"]
}
