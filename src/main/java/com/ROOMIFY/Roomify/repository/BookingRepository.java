package com.ROOMIFY.Roomify.repository;

import com.ROOMIFY.Roomify.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ========== ORIGINAL METHODS (UNCHANGED) ==========

    List<Booking> findByUserId(Long userId);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.room.id = :roomId")
    List<Booking> findByUserIdAndRoomId(@Param("userId") Long userId, @Param("roomId") Long roomId);

    @Query("SELECT b FROM Booking b WHERE b.room.postedBy = :ownerId")
    List<Booking> findByRoomPostedBy(@Param("ownerId") Long ownerId);

    List<Booking> findByRoomId(Long roomId);

    // ========== ORIGINAL CONSISTENCY METHODS ==========

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.room.id = :roomId")
    boolean existsAnyBookingByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.room.id = :roomId AND (b.status = 'ACCEPTED' OR b.status = 'CONFIRMED')")
    boolean existsActiveBookingByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.room.id = :roomId")
    int countAllBookingsByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.room.id = :roomId AND b.status = :status")
    int countBookingsByRoomIdAndStatus(@Param("roomId") Long roomId, @Param("status") String status);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.room.id = :roomId AND b.user.id != :userId")
    boolean existsOtherUserBooking(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // ========== ANALYTICS METHODS (NATIVE QUERIES - MOST RELIABLE) ==========

    @Query(value = "SELECT " +
            "CASE " +
            "   WHEN r.price < 50 THEN 'Under $50' " +
            "   WHEN r.price BETWEEN 50 AND 100 THEN '$50 - $100' " +
            "   WHEN r.price BETWEEN 101 AND 200 THEN '$101 - $200' " +
            "   WHEN r.price BETWEEN 201 AND 500 THEN '$201 - $500' " +
            "   ELSE 'Over $500' " +
            "END as price_range, " +
            "COUNT(b.id), AVG(r.price) " +
            "FROM bookings b " +
            "JOIN rooms r ON b.room_id = r.id " +
            "WHERE b.status IN ('APPROVED', 'ACCEPTED', 'CONFIRMED') " +
            "GROUP BY price_range " +
            "ORDER BY COUNT(b.id) DESC",
            nativeQuery = true)
    List<Object[]> getBookingCountsByPriceRange();

    @Query(value = "SELECT " +
            "SUBSTRING_INDEX(r.address, ',', 1) as area, " +
            "COUNT(b.id), AVG(r.price), " +
            "(COUNT(b.id) * 100.0 / (SELECT COUNT(*) FROM bookings WHERE status IN ('APPROVED', 'ACCEPTED', 'CONFIRMED'))) " +
            "FROM bookings b " +
            "JOIN rooms r ON b.room_id = r.id " +
            "WHERE b.status IN ('APPROVED', 'ACCEPTED', 'CONFIRMED') " +
            "AND r.address IS NOT NULL AND r.address != '' " +
            "GROUP BY area " +
            "ORDER BY COUNT(b.id) DESC",
            nativeQuery = true)
    List<Object[]> getBookingCountsByArea();

    @Query(value = "SELECT " +
            "DATE_FORMAT(b.created_at, '%Y-%m') as month, " +
            "COUNT(b.id), SUM(r.price) " +
            "FROM bookings b " +
            "JOIN rooms r ON b.room_id = r.id " +
            "WHERE b.status IN ('APPROVED', 'ACCEPTED', 'CONFIRMED') " +
            "AND b.created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH) " +
            "GROUP BY DATE_FORMAT(b.created_at, '%Y-%m') " +
            "ORDER BY month DESC",
            nativeQuery = true)
    List<Object[]> getMonthlyBookingTrends();

    @Query(value = "SELECT " +
            "r.price, COUNT(b.id) " +
            "FROM bookings b " +
            "JOIN rooms r ON b.room_id = r.id " +
            "WHERE b.status IN ('APPROVED', 'ACCEPTED', 'CONFIRMED') " +
            "GROUP BY r.price " +
            "ORDER BY COUNT(b.id) DESC " +
            "LIMIT 10",
            nativeQuery = true)
    List<Object[]> getMostFrequentExactPrices();

    @Query(value = "SELECT " +
            "r.title, r.id, COUNT(b.id), SUM(r.price) " +
            "FROM bookings b " +
            "JOIN rooms r ON b.room_id = r.id " +
            "WHERE b.status IN ('APPROVED', 'ACCEPTED', 'CONFIRMED') " +
            "GROUP BY r.id " +
            "ORDER BY SUM(r.price) DESC " +
            "LIMIT 10",
            nativeQuery = true)
    List<Object[]> getTopRevenueRooms();

    @Query(value = "SELECT " +
            "MONTHNAME(b.created_at) as month, " +
            "SUM(r.price) " +
            "FROM bookings b " +
            "JOIN rooms r ON b.room_id = r.id " +
            "WHERE b.status IN ('APPROVED', 'ACCEPTED', 'CONFIRMED') " +
            "AND YEAR(b.created_at) = YEAR(CURDATE()) " +
            "GROUP BY MONTH(b.created_at) " +
            "ORDER BY MONTH(b.created_at)",
            nativeQuery = true)
    List<Object[]> getMonthlyRevenueCurrentYear();

    @Query(value = "SELECT " +
            "b.status, COUNT(*) " +
            "FROM bookings b " +
            "GROUP BY b.status",
            nativeQuery = true)
    List<Object[]> getBookingStatusDistribution();

    @Query(value = "SELECT " +
            "AVG(TIMESTAMPDIFF(HOUR, b.created_at, b.updated_at)) " +
            "FROM bookings b " +
            "WHERE b.status IN ('APPROVED', 'ACCEPTED', 'CONFIRMED')",
            nativeQuery = true)
    Double getAverageApprovalTimeHours();

    @Query(value = "SELECT " +
            "u.name, u.email, COUNT(b.id) " +
            "FROM bookings b " +
            "JOIN users u ON b.user_id = u.id " +
            "WHERE b.status IN ('APPROVED', 'ACCEPTED', 'CONFIRMED') " +
            "GROUP BY u.id " +
            "ORDER BY COUNT(b.id) DESC " +
            "LIMIT 5",
            nativeQuery = true)
    List<Object[]> getTopActiveUsers();

    @Query(value = "SELECT " +
            "DAYNAME(b.created_at) as day_of_week, " +
            "COUNT(b.id) " +
            "FROM bookings b " +
            "WHERE b.status IN ('APPROVED', 'ACCEPTED', 'CONFIRMED') " +
            "GROUP BY DAYOFWEEK(b.created_at) " +
            "ORDER BY COUNT(b.id) DESC",
            nativeQuery = true)
    List<Object[]> getBookingsByDayOfWeek();

    @Query(value = "SELECT " +
            "(SELECT COUNT(*) FROM bookings WHERE status = 'CANCELLED') * 100.0 / COUNT(*) " +
            "FROM bookings",
            nativeQuery = true)
    Double getCancellationRate();

    @Query(value = "SELECT " +
            "SUBSTRING_INDEX(r.address, ',', 1) as area, " +
            "AVG(r.price), COUNT(b.id) " +
            "FROM bookings b " +
            "JOIN rooms r ON b.room_id = r.id " +
            "WHERE b.status IN ('APPROVED', 'ACCEPTED', 'CONFIRMED') " +
            "AND r.address IS NOT NULL AND r.address != '' " +
            "GROUP BY area " +
            "ORDER BY AVG(r.price) DESC",
            nativeQuery = true)
    List<Object[]> getAveragePriceByArea();

    @Query(value = "SELECT b.status, COUNT(*) FROM bookings b WHERE b.room_id = :roomId GROUP BY b.status", nativeQuery = true)
    List<Object[]> getBookingStatusCountsByRoom(@Param("roomId") Long roomId);

    @Query(value = "SELECT * FROM bookings b WHERE b.created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<Booking> findBookingsBetweenDatesNative(@Param("startDate") String startDate, @Param("endDate") String endDate);
}