package com.ROOMIFY.Roomify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDTO {
    private String name;
    private long count;
    private double value;
}
