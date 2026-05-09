package com.ROOMIFY.Roomify.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceRangeDTO {
    private String priceRange;
    private long bookingCount;
    private double averagePrice;
}
