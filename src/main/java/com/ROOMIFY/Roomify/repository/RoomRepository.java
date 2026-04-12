package com.ROOMIFY.Roomify.repository;

import com.ROOMIFY.Roomify.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    public enum RoomStatus {
        AVAILABLE,
        BOOKED,
        PENDING,
        INACTIVE
    }

    // Find rooms posted by a specific user (owner) - CORRECT
    List<Room> findByPostedBy(Long postedBy);

    // Find all available rooms
    List<Room> findByIsAvailableTrue();

    // REMOVE THIS - it doesn't exist in Room entity
    // List<Room> findByOwnerId(Long ownerId);  // DELETE THIS LINE

    // Find all rooms by status
    List<Room> findByStatus(RoomStatus status);
}