package com.ROOMIFY.Roomify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaBookingDTO {
    private String area;
    private long bookingCount;
    private double averagePrice;
    private double occupancyRate;
}