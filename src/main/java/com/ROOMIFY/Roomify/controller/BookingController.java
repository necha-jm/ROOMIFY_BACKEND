package com.ROOMIFY.Roomify.controller;

import com.ROOMIFY.Roomify.dto.ApiResponse;
import com.ROOMIFY.Roomify.dto.BookingRequestDto;
import com.ROOMIFY.Roomify.dto.BookingResponseDTO;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public ResponseEntity<ApiResponse<BookingResponseDTO>> createBooking(@RequestBody BookingRequestDto bookingRequest) {
        try {
            // FIRST: Check if room already has an ACTIVE booking
            if (bookingRepository.existsActiveBookingByRoomId(bookingRequest.getRoomId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiResponse<>(false, null, "Room already has an active booking"));
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate startLocalDate = LocalDate.parse(bookingRequest.getStartDate(), formatter);
            LocalDate endLocalDate = LocalDate.parse(bookingRequest.getEndDate(), formatter);

            LocalDateTime startDate = startLocalDate.atStartOfDay();
            LocalDateTime endDate = endLocalDate.atStartOfDay();

            Room room = roomRepository.findById(bookingRequest.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Room not found with id: " + bookingRequest.getRoomId()));

            User user = userRepository.findById(bookingRequest.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + bookingRequest.getUserId()));

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

            BookingResponseDTO responseDTO = convertToDTO(savedBooking);

            return ResponseEntity.ok(new ApiResponse<>(true, responseDTO, "Booking created successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, null, "Failed to create booking: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<BookingResponseDTO>>> getUserBookings(@PathVariable Long userId) {
        try {
            List<Booking> bookings = bookingRepository.findByUserId(userId);

            List<BookingResponseDTO> dtos = bookings.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse<>(true, dtos, "Bookings retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to retrieve bookings: " + e.getMessage()));
        }
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<ApiResponse<List<BookingResponseDTO>>> getOwnerBookings(@PathVariable Long ownerId) {
        try {
            List<Room> rooms = roomRepository.findByPostedBy(ownerId);

            List<BookingResponseDTO> allBookings = new ArrayList<>();

            if (rooms != null && !rooms.isEmpty()) {
                for (Room room : rooms) {
                    List<Booking> roomBookings = bookingRepository.findByRoomId(room.getId());
                    for (Booking booking : roomBookings) {
                        allBookings.add(convertToDTO(booking));
                    }
                }
            }

            return ResponseEntity.ok(new ApiResponse<>(true, allBookings, "Bookings retrieved successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to retrieve bookings: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/room/{roomId}")
    public ResponseEntity<ApiResponse<List<BookingResponseDTO>>> checkUserBooking(
            @PathVariable Long userId,
            @PathVariable Long roomId) {
        try {
            List<Booking> bookings = bookingRepository.findByUserIdAndRoomId(userId, roomId);

            List<BookingResponseDTO> dtos = bookings.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse<>(true, dtos, "Check completed"));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse<>(true, new ArrayList<>(), "No bookings found"));
        }
    }

    // ========== NEW ENDPOINTS FOR CONSISTENCY ==========

    // Check if room has ANY booking (by ANY user)
    @GetMapping("/room/{roomId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> isRoomBooked(@PathVariable Long roomId) {
        try {
            boolean hasAnyBooking = bookingRepository.existsAnyBookingByRoomId(roomId);
            return ResponseEntity.ok(new ApiResponse<>(true, hasAnyBooking,
                    hasAnyBooking ? "Room has bookings" : "Room has no bookings"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, false, "Error checking room: " + e.getMessage()));
        }
    }

    // Check if room has ACTIVE booking (ACCEPTED/CONFIRMED)
    @GetMapping("/room/{roomId}/active-exists")
    public ResponseEntity<ApiResponse<Boolean>> isRoomActiveBooking(@PathVariable Long roomId) {
        try {
            boolean hasActiveBooking = bookingRepository.existsActiveBookingByRoomId(roomId);
            return ResponseEntity.ok(new ApiResponse<>(true, hasActiveBooking,
                    hasActiveBooking ? "Room has active booking" : "Room has no active booking"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, false, "Error checking room: " + e.getMessage()));
        }
    }

    // Get count of ALL bookings for a room
    @GetMapping("/room/{roomId}/count")
    public ResponseEntity<ApiResponse<Integer>> getRoomBookingsCount(@PathVariable Long roomId) {
        try {
            int count = bookingRepository.countAllBookingsByRoomId(roomId);
            return ResponseEntity.ok(new ApiResponse<>(true, count, "Count retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, 0, "Error getting count: " + e.getMessage()));
        }
    }

    // Get detailed room booking status (for map color logic)
    @GetMapping("/room/{roomId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoomBookingStatus(@PathVariable Long roomId) {
        try {
            Map<String, Object> status = new HashMap<>();

            // Total bookings count
            int totalBookings = bookingRepository.countAllBookingsByRoomId(roomId);
            status.put("totalBookings", totalBookings);

            // Has any booking
            status.put("hasAnyBooking", totalBookings > 0);

            // Has active booking (ACCEPTED/CONFIRMED)
            boolean hasActiveBooking = bookingRepository.existsActiveBookingByRoomId(roomId);
            status.put("hasActiveBooking", hasActiveBooking);

            // Count by status
            status.put("pendingCount", bookingRepository.countBookingsByRoomIdAndStatus(roomId, "PENDING"));
            status.put("acceptedCount", bookingRepository.countBookingsByRoomIdAndStatus(roomId, "ACCEPTED"));
            status.put("confirmedCount", bookingRepository.countBookingsByRoomIdAndStatus(roomId, "CONFIRMED"));
            status.put("rejectedCount", bookingRepository.countBookingsByRoomIdAndStatus(roomId, "REJECTED"));
            status.put("cancelledCount", bookingRepository.countBookingsByRoomIdAndStatus(roomId, "CANCELLED"));

            return ResponseEntity.ok(new ApiResponse<>(true, status, "Status retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Error getting status: " + e.getMessage()));
        }
    }

    // Check if room has booking by other users (for new user login)
    @GetMapping("/room/{roomId}/other-user-booking")
    public ResponseEntity<ApiResponse<Boolean>> hasOtherUserBooking(
            @PathVariable Long roomId,
            @RequestParam Long currentUserId) {
        try {
            boolean hasOtherBooking = bookingRepository.existsOtherUserBooking(roomId, currentUserId);
            return ResponseEntity.ok(new ApiResponse<>(true, hasOtherBooking,
                    hasOtherBooking ? "Room booked by other user" : "No other user bookings"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, false, "Error checking: " + e.getMessage()));
        }
    }

    // ========== EXISTING ENDPOINTS ==========

    @PutMapping("/{bookingId}/accept")
    public ResponseEntity<ApiResponse<BookingResponseDTO>> acceptBooking(@PathVariable Long bookingId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
            booking.setStatus("ACCEPTED");
            booking.setUpdatedAt(LocalDateTime.now());
            Booking savedBooking = bookingRepository.save(booking);

            BookingResponseDTO responseDTO = convertToDTO(savedBooking);

            return ResponseEntity.ok(new ApiResponse<>(true, responseDTO, "Booking accepted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to accept booking: " + e.getMessage()));
        }
    }

    @PutMapping("/{bookingId}/reject")
    public ResponseEntity<ApiResponse<BookingResponseDTO>> rejectBooking(@PathVariable Long bookingId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
            booking.setStatus("REJECTED");
            booking.setUpdatedAt(LocalDateTime.now());
            Booking savedBooking = bookingRepository.save(booking);

            BookingResponseDTO responseDTO = convertToDTO(savedBooking);

            return ResponseEntity.ok(new ApiResponse<>(true, responseDTO, "Booking rejected successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to reject booking: " + e.getMessage()));
        }
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponseDTO>> cancelBooking(@PathVariable Long bookingId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
            booking.setStatus("CANCELLED");
            booking.setUpdatedAt(LocalDateTime.now());
            Booking savedBooking = bookingRepository.save(booking);

            BookingResponseDTO responseDTO = convertToDTO(savedBooking);

            return ResponseEntity.ok(new ApiResponse<>(true, responseDTO, "Booking cancelled successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to cancel booking: " + e.getMessage()));
        }
    }

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

    private BookingResponseDTO convertToDTO(Booking booking) {
        BookingResponseDTO dto = new BookingResponseDTO();

        dto.setId(booking.getId());
        dto.setStatus(booking.getStatus());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setStartDate(booking.getStartDate());
        dto.setEndDate(booking.getEndDate());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setNumberOfGuests(booking.getNumberOfGuests());
        dto.setSpecialRequests(booking.getSpecialRequests());

        User user = booking.getUser();
        if (user != null) {
            dto.setUserId(user.getId());
            dto.setUserName(user.getName());
            dto.setUserEmail(user.getEmail());
        }

        Room room = booking.getRoom();
        if (room != null) {
            dto.setRoomId(room.getId());
            dto.setRoomTitle(room.getTitle());
        }

        return dto;
    }
}