package com.smartparking.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInRequest {
    private String licensePlate;
    private String vehicleType;
    private String ownerName;
    private String ownerContact;
}
