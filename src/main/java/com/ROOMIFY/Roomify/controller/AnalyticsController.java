package com.ROOMIFY.Roomify.controller;

import com.ROOMIFY.Roomify.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/price-ranges")
    public ResponseEntity<Map<String, Object>> getMostFrequentPriceRanges() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", analyticsService.getMostFrequentPriceRanges());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/most-booked-areas")
    public ResponseEntity<Map<String, Object>> getMostBookedAreas() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", analyticsService.getMostBookedAreas());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exact-prices")
    public ResponseEntity<Map<String, Object>> getMostFrequentExactPrices() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", analyticsService.getMostFrequentExactPrices());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly-trends")
    public ResponseEntity<Map<String, Object>> getMonthlyTrends() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", analyticsService.getMonthlyBookingTrends());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-rooms")
    public ResponseEntity<Map<String, Object>> getTopRevenueRooms() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", analyticsService.getTopRevenueRooms());
        return ResponseEntity.ok(response);
    }
}
