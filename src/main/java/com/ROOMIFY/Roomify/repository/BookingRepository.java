package com.ROOMIFY.Roomify.repository;

import com.ROOMIFY.Roomify.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.room.id = :roomId")
    List<Booking> findByUserIdAndRoomId(@Param("userId") Long userId, @Param("roomId") Long roomId);

    @Query("SELECT b FROM Booking b WHERE b.room.postedBy = :ownerId")
    List<Booking> findByRoomPostedBy(@Param("ownerId") Long ownerId);

    List<Booking> findByRoomId(Long roomId);

    // ========== NEW METHODS FOR CONSISTENCY ==========

    // Check if room has ANY booking (regardless of user)
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.room.id = :roomId")
    boolean existsAnyBookingByRoomId(@Param("roomId") Long roomId);

    // Check if room has ACTIVE booking (ACCEPTED or CONFIRMED)
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.room.id = :roomId AND (b.status = 'ACCEPTED' OR b.status = 'CONFIRMED')")
    boolean existsActiveBookingByRoomId(@Param("roomId") Long roomId);

    // Get count of ALL bookings for a room
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.room.id = :roomId")
    int countAllBookingsByRoomId(@Param("roomId") Long roomId);

    // Get count of bookings by status
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.room.id = :roomId AND b.status = :status")
    int countBookingsByRoomIdAndStatus(@Param("roomId") Long roomId, @Param("status") String status);

    // Check if room has ANY booking except the current user's
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.room.id = :roomId AND b.user.id != :userId")
    boolean existsOtherUserBooking(@Param("roomId") Long roomId, @Param("userId") Long userId);
}