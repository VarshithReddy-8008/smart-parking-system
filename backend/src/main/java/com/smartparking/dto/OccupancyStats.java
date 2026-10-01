package com.smartparking.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
@Builder
public class OccupancyStats {
    private long totalSlots;
    private long occupiedSlots;
    private long availableSlots;
    private Map<String, Long> availableByType;
    private Map<String, Long> occupiedByType;
}
