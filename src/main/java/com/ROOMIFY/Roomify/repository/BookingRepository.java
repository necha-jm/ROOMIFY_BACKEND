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
}