package com.ROOMIFY.Roomify.service;

import com.ROOMIFY.Roomify.dto.AreaBookingDTO;
import com.ROOMIFY.Roomify.dto.PriceRangeDTO;
import com.ROOMIFY.Roomify.model.Booking;
import com.ROOMIFY.Roomify.model.BookingStatus;
import com.ROOMIFY.Roomify.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    @Autowired
    private BookingRepository bookingRepository;

    // ========== PRICE RANGE ANALYTICS ==========

    public List<PriceRangeDTO> getMostFrequentPriceRanges() {
        List<Object[]> results = bookingRepository.getBookingCountsByPriceRange();
        List<PriceRangeDTO> priceRanges = new ArrayList<>();

        for (Object[] result : results) {
            PriceRangeDTO dto = new PriceRangeDTO();
            dto.setPriceRange((String) result[0]);
            dto.setBookingCount(((Number) result[1]).longValue());
            dto.setAveragePrice(((Number) result[2]).doubleValue());
            priceRanges.add(dto);
        }

        return priceRanges;
    }

    public List<PriceRangeDTO> getMostFrequentExactPrices() {
        List<Object[]> results = bookingRepository.getMostFrequentExactPrices();
        List<PriceRangeDTO> prices = new ArrayList<>();

        for (Object[] result : results) {
            PriceRangeDTO dto = new PriceRangeDTO();
            dto.setPriceRange("$" + ((Number) result[0]).doubleValue());
            dto.setBookingCount(((Number) result[1]).longValue());
            dto.setAveragePrice(((Number) result[0]).doubleValue());
            prices.add(dto);
        }

        return prices;
    }

    // ========== AREA ANALYTICS ==========

    public List<AreaBookingDTO> getMostBookedAreas() {
        List<Object[]> results = bookingRepository.getBookingCountsByArea();
        List<AreaBookingDTO> areas = new ArrayList<>();

        for (Object[] result : results) {
            AreaBookingDTO dto = new AreaBookingDTO();
            dto.setArea((String) result[0]);
            dto.setBookingCount(((Number) result[1]).longValue());
            dto.setAveragePrice(((Number) result[2]).doubleValue());
            dto.setOccupancyRate(((Number) result[3]).doubleValue());
            areas.add(dto);
        }

        return areas;
    }

    public List<AreaBookingDTO> getAveragePriceByArea() {
        List<Object[]> results = bookingRepository.getAveragePriceByArea();
        List<AreaBookingDTO> areas = new ArrayList<>();

        for (Object[] result : results) {
            AreaBookingDTO dto = new AreaBookingDTO();
            dto.setArea((String) result[0]);
            dto.setAveragePrice(((Number) result[1]).doubleValue());
            dto.setBookingCount(((Number) result[2]).longValue());
            areas.add(dto);
        }

        return areas;
    }

    // ========== TREND ANALYTICS ==========

    public List<Map<String, Object>> getMonthlyBookingTrends() {
        List<Object[]> results = bookingRepository.getMonthlyBookingTrends();
        List<Map<String, Object>> trends = new ArrayList<>();

        for (Object[] result : results) {
            Map<String, Object> trend = new HashMap<>();
            trend.put("month", result[0]);
            trend.put("bookingCount", ((Number) result[1]).longValue());
            trend.put("totalRevenue", ((Number) result[2]).doubleValue());
            trends.add(trend);
        }

        return trends;
    }

    public List<Map<String, Object>> getMonthlyRevenueCurrentYear() {
        List<Object[]> results = bookingRepository.getMonthlyRevenueCurrentYear();
        List<Map<String, Object>> revenues = new ArrayList<>();

        for (Object[] result : results) {
            Map<String, Object> revenue = new HashMap<>();
            revenue.put("month", result[0]);
            revenue.put("totalRevenue", ((Number) result[1]).doubleValue());
            revenues.add(revenue);
        }

        return revenues;
    }

    // ========== ROOM ANALYTICS ==========

    public List<Map<String, Object>> getTopRevenueRooms() {
        List<Object[]> results = bookingRepository.getTopRevenueRooms();
        List<Map<String, Object>> topRooms = new ArrayList<>();

        for (Object[] result : results) {
            Map<String, Object> room = new HashMap<>();
            room.put("title", result[0]);
            room.put("roomId", result[1]);
            room.put("bookingCount", ((Number) result[2]).longValue());
            room.put("totalRevenue", ((Number) result[3]).doubleValue());
            topRooms.add(room);
        }

        return topRooms;
    }

    // ========== STATUS DISTRIBUTION ==========

    public List<Map<String, Object>> getBookingStatusDistribution() {
        List<Object[]> results = bookingRepository.getBookingStatusDistribution();
        List<Map<String, Object>> distribution = new ArrayList<>();

        for (Object[] result : results) {
            Map<String, Object> status = new HashMap<>();
            status.put("status", result[0]);
            status.put("count", ((Number) result[1]).longValue());
            distribution.add(status);
        }

        return distribution;
    }

    public Map<String, Object> getCancellationRate() {
        Double rate = bookingRepository.getCancellationRate();
        Map<String, Object> cancellationStats = new HashMap<>();
        cancellationStats.put("cancellationRate", rate != null ? rate : 0.0);
        cancellationStats.put("cancellationRateFormatted", String.format("%.2f%%", rate != null ? rate : 0.0));
        return cancellationStats;
    }

    public List<Map<String, Object>> getBookingStatusCountsByRoom(Long roomId) {
        List<Object[]> results = bookingRepository.getBookingStatusCountsByRoom(roomId);
        List<Map<String, Object>> statusCounts = new ArrayList<>();

        for (Object[] result : results) {
            Map<String, Object> status = new HashMap<>();
            status.put("status", result[0]);
            status.put("count", ((Number) result[1]).longValue());
            statusCounts.add(status);
        }

        return statusCounts;
    }

    // ========== USER ANALYTICS ==========

    public List<Map<String, Object>> getTopActiveUsers() {
        List<Object[]> results = bookingRepository.getTopActiveUsers();
        List<Map<String, Object>> topUsers = new ArrayList<>();

        for (Object[] result : results) {
            Map<String, Object> user = new HashMap<>();
            user.put("name", result[0]);
            user.put("email", result[1]);
            user.put("bookingCount", ((Number) result[2]).longValue());
            topUsers.add(user);
        }

        return topUsers;
    }

    // ========== TIME-BASED ANALYTICS ==========

    public List<Map<String, Object>> getBookingsByDayOfWeek() {
        List<Object[]> results = bookingRepository.getBookingsByDayOfWeek();
        List<Map<String, Object>> dayStats = new ArrayList<>();

        for (Object[] result : results) {
            Map<String, Object> day = new HashMap<>();
            day.put("day", result[0]);
            day.put("bookingCount", ((Number) result[1]).longValue());
            dayStats.add(day);
        }

        return dayStats;
    }

    public Map<String, Object> getAverageApprovalTime() {
        Double avgHours = bookingRepository.getAverageApprovalTimeHours();
        Map<String, Object> approvalStats = new HashMap<>();
        approvalStats.put("averageApprovalTimeHours", avgHours != null ? avgHours : 0.0);
        approvalStats.put("averageApprovalTimeFormatted", formatHours(avgHours != null ? avgHours : 0.0));
        return approvalStats;
    }

    public List<Booking> getBookingsBetweenDates(String startDate, String endDate) {
        return bookingRepository.findBookingsBetweenDatesNative(startDate, endDate);
    }

    // ========== SUMMARY DASHBOARD ==========

    public Map<String, Object> getAnalyticsDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        dashboard.put("mostFrequentPriceRanges", getMostFrequentPriceRanges());
        dashboard.put("mostBookedAreas", getMostBookedAreas());
        dashboard.put("monthlyTrends", getMonthlyBookingTrends());
        dashboard.put("topRevenueRooms", getTopRevenueRooms());
        dashboard.put("bookingStatusDistribution", getBookingStatusDistribution());
        dashboard.put("cancellationRate", getCancellationRate());
        dashboard.put("topActiveUsers", getTopActiveUsers());
        dashboard.put("bookingsByDayOfWeek", getBookingsByDayOfWeek());
        dashboard.put("averageApprovalTime", getAverageApprovalTime());
        dashboard.put("totalBookings", getTotalBookingsCount());
        dashboard.put("totalRevenue", getTotalRevenue());
        dashboard.put("pendingBookings", getPendingBookingsCount());

        return dashboard;
    }

    // ========== HELPER METHODS ==========

    public long getTotalBookingsCount() {
        return bookingRepository.count();
    }

    public long getPendingBookingsCount() {
        // Count pending bookings using existing repository methods
        List<Booking> allBookings = bookingRepository.findAll();
        return allBookings.stream()
                .filter(b -> b.getStatus().equals(BookingStatus.PENDING))
                .count();
    }

    public long getApprovedBookingsCount() {
        // Count approved bookings using existing methods
        List<Object[]> statusDistribution = bookingRepository.getBookingStatusDistribution();
        for (Object[] status : statusDistribution) {
            String statusName = (String) status[0];
            if ("APPROVED".equals(statusName) || "ACCEPTED".equals(statusName) || "CONFIRMED".equals(statusName)) {
                return ((Number) status[1]).longValue();
            }
        }
        return 0;
    }

    public double getTotalRevenue() {
        List<Object[]> topRooms = bookingRepository.getTopRevenueRooms();
        double totalRevenue = 0;
        for (Object[] room : topRooms) {
            totalRevenue += ((Number) room[3]).doubleValue();
        }
        return totalRevenue;
    }

    public double getAverageBookingValue() {
        long totalBookings = getTotalBookingsCount();
        if (totalBookings == 0) return 0;
        return getTotalRevenue() / totalBookings;
    }

    public Map<String, Object> getQuickStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBookings", getTotalBookingsCount());
        stats.put("approvedBookings", getApprovedBookingsCount());
        stats.put("pendingBookings", getPendingBookingsCount());
        stats.put("totalRevenue", String.format("$%.2f", getTotalRevenue()));
        stats.put("averageBookingValue", String.format("$%.2f", getAverageBookingValue()));
        return stats;
    }

    private String formatHours(double hours) {
        if (hours < 1) {
            int minutes = (int) (hours * 60);
            return minutes + " minute" + (minutes != 1 ? "s" : "");
        } else if (hours < 24) {
            int hrs = (int) hours;
            return hrs + " hour" + (hrs != 1 ? "s" : "");
        } else {
            int days = (int) (hours / 24);
            int remainingHours = (int) (hours % 24);
            if (remainingHours > 0) {
                return days + " day" + (days != 1 ? "s" : "") + " and " + remainingHours + " hour" + (remainingHours != 1 ? "s" : "");
            }
            return days + " day" + (days != 1 ? "s" : "");
        }
    }
}