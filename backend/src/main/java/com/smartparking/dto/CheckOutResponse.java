package com.smartparking.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CheckOutResponse {
    private Long sessionId;
    private String licensePlate;
    private String slotNumber;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Long durationInMinutes;
    private BigDecimal amountDue;
    private Long paymentId;
    private String paymentStatus;
}
