package com.ROOMIFY.Roomify.controller;

import com.ROOMIFY.Roomify.dto.ApiResponse;
import com.ROOMIFY.Roomify.dto.BookingRequestDto;
import com.ROOMIFY.Roomify.model.Booking;
import com.ROOMIFY.Roomify.model.Room;
import com.ROOMIFY.Roomify.model.User;
import com.ROOMIFY.Roomify.repository.BookingRepository;
import com.ROOMIFY.Roomify.repository.RoomRepository;
import com.ROOMIFY.Roomify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Booking>> createBooking(@RequestBody BookingRequestDto bookingRequest) {
        try {
            // FIXED: Parse as LocalDate first, then convert to LocalDateTime at start of day
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate startLocalDate = LocalDate.parse(bookingRequest.getStartDate(), formatter);
            LocalDate endLocalDate = LocalDate.parse(bookingRequest.getEndDate(), formatter);

            LocalDateTime startDate = startLocalDate.atStartOfDay();  // Converts to 2026-04-08T00:00:00
            LocalDateTime endDate = endLocalDate.atStartOfDay();      // Converts to 2026-05-08T00:00:00

            // Validate room exists
            Room room = roomRepository.findById(bookingRequest.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Room not found with id: " + bookingRequest.getRoomId()));

            // Validate user exists
            User user = userRepository.findById(bookingRequest.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + bookingRequest.getUserId()));

            // Create new booking
            Booking booking = new Booking();
            booking.setRoom(room);
            booking.setUser(user);
            booking.setStatus(bookingRequest.getStatus() != null ? bookingRequest.getStatus() : "PENDING");
            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());
            booking.setTotalPrice(bookingRequest.getTotalPrice());
            booking.setStartDate(startDate);
            booking.setEndDate(endDate);
            booking.setNumberOfGuests(bookingRequest.getNumberOfGuests());
            booking.setSpecialRequests(bookingRequest.getSpecialRequests());

            Booking savedBooking = bookingRepository.save(booking);

            return ResponseEntity.ok(new ApiResponse<>(true, savedBooking, "Booking created successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, null, "Failed to create booking: " + e.getMessage()));
        }
    }

    // GET endpoint for user bookings
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Booking>>> getUserBookings(@PathVariable Long userId) {
        try {
            List<Booking> bookings = bookingRepository.findByUserId(userId);
            return ResponseEntity.ok(new ApiResponse<>(true, bookings, "Bookings retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to retrieve bookings: " + e.getMessage()));
        }
    }

    // GET endpoint to check if user has booking for a room
    @GetMapping("/user/{userId}/room/{roomId}")
    public ResponseEntity<ApiResponse<List<Booking>>> checkUserBooking(
            @PathVariable Long userId,
            @PathVariable Long roomId) {
        try {
            List<Booking> bookings = bookingRepository.findByUserIdAndRoomId(userId, roomId);
            return ResponseEntity.ok(new ApiResponse<>(true, bookings, "Check completed"));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse<>(true, new java.util.ArrayList<>(), "No bookings found"));
        }
    }

    // PUT endpoint to accept booking
    @PutMapping("/{bookingId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptBooking(@PathVariable Long bookingId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
            booking.setStatus("ACCEPTED");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            return ResponseEntity.ok(new ApiResponse<>(true, null, "Booking accepted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to accept booking: " + e.getMessage()));
        }
    }

    // PUT endpoint to reject booking
    @PutMapping("/{bookingId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectBooking(@PathVariable Long bookingId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
            booking.setStatus("REJECTED");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            return ResponseEntity.ok(new ApiResponse<>(true, null, "Booking rejected successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to reject booking: " + e.getMessage()));
        }
    }

    // PUT endpoint to cancel booking
    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(@PathVariable Long bookingId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
            booking.setStatus("CANCELLED");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            return ResponseEntity.ok(new ApiResponse<>(true, null, "Booking cancelled successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to cancel booking: " + e.getMessage()));
        }
    }

    // DELETE endpoint to delete booking
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<Void>> deleteBooking(@PathVariable Long bookingId) {
        try {
            bookingRepository.deleteById(bookingId);
            return ResponseEntity.ok(new ApiResponse<>(true, null, "Booking deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to delete booking: " + e.getMessage()));
        }
    }
}