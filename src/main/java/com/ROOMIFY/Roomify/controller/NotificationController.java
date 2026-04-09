package com.ROOMIFY.Roomify.controller;

import com.ROOMIFY.Roomify.dto.ApiResponse;
import com.ROOMIFY.Roomify.model.NotificationToken;
import com.ROOMIFY.Roomify.model.Room;
import com.ROOMIFY.Roomify.model.User;
import com.ROOMIFY.Roomify.repository.NotificationTokenRepository;
import com.ROOMIFY.Roomify.repository.RoomRepository;
import com.ROOMIFY.Roomify.repository.UserRepository;
import com.ROOMIFY.Roomify.service.FCMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private FCMService fcmService;

    @Autowired
    private NotificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    // Register or update FCM token
    @PostMapping("/register-token")
    public ResponseEntity<ApiResponse<Void>> registerToken(
            @RequestParam Long userId,
            @RequestParam String fcmToken,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String deviceModel,
            @RequestParam(required = false) String osVersion,
            @RequestParam(required = false) String appVersion) {

        try {
            // Check if user exists
            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, null, "User not found"));
            }

            // Find existing token or create new
            Optional<NotificationToken> existingToken = tokenRepository.findByUserId(userId);
            NotificationToken token;

            if (existingToken.isPresent()) {
                token = existingToken.get();
                token.setFcmToken(fcmToken);
            } else {
                token = new NotificationToken();
                token.setUserId(userId);
                token.setFcmToken(fcmToken);
            }

            // Set device info if provided
            if (deviceId != null) token.setDeviceId(deviceId);
            if (deviceModel != null) token.setDeviceModel(deviceModel);
            if (osVersion != null) token.setOsVersion(osVersion);
            if (appVersion != null) token.setAppVersion(appVersion);

            token.setActive(true);
            token.setUpdatedAt(LocalDateTime.now());

            tokenRepository.save(token);

            return ResponseEntity.ok(new ApiResponse<>(true, null, "Token registered successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to register token: " + e.getMessage()));
        }
    }

    // Unregister token (when user logs out)
    @DeleteMapping("/unregister-token")
    public ResponseEntity<ApiResponse<Void>> unregisterToken(@RequestParam Long userId) {
        try {
            tokenRepository.deleteByUserId(userId);
            return ResponseEntity.ok(new ApiResponse<>(true, null, "Token unregistered successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to unregister token: " + e.getMessage()));
        }
    }

    // Send notification to specific user
    @PostMapping("/send-to-user")
    public ResponseEntity<ApiResponse<Void>> sendNotificationToUser(
            @RequestParam Long userId,
            @RequestParam String title,
            @RequestParam String body,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String roomId,
            @RequestParam(required = false) String bookingId) {

        try {
            Optional<NotificationToken> tokenOpt = tokenRepository.findByUserId(userId);
            if (tokenOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, null, "User has no registered token"));
            }

            Map<String, String> data = new HashMap<>();
            if (type != null) data.put("type", type);
            if (roomId != null) data.put("roomId", roomId);
            if (bookingId != null) data.put("bookingId", bookingId);

            fcmService.sendNotificationToUser(tokenOpt.get().getFcmToken(), title, body, data);

            return ResponseEntity.ok(new ApiResponse<>(true, null, "Notification sent successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to send notification: " + e.getMessage()));
        }
    }

    // Send notification when new room is posted
    @PostMapping("/send-room-notification")
    public ResponseEntity<ApiResponse<Void>> sendRoomNotification(@RequestParam Long roomId) {
        try {
            Room room = roomRepository.findById(roomId).orElse(null);
            if (room == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, null, "Room not found"));
            }

            // Get all active tokens
            List<NotificationToken> tokens = tokenRepository.findAllActiveTokens();

            int successCount = 0;
            for (NotificationToken token : tokens) {
                Map<String, String> data = new HashMap<>();
                data.put("type", "new_room");
                data.put("roomId", String.valueOf(roomId));
                data.put("title", room.getTitle());

                fcmService.sendNotificationToUser(
                        token.getFcmToken(),
                        "New Room Available!",
                        room.getTitle() + " - $" + room.getPrice() + "/month",
                        data
                );
                successCount++;
            }

            return ResponseEntity.ok(new ApiResponse<>(true, null,
                    "Notification sent to " + successCount + " users"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to send notifications: " + e.getMessage()));
        }
    }

    // Send booking request notification to owner
    @PostMapping("/send-booking-notification")
    public ResponseEntity<ApiResponse<Void>> sendBookingNotification(
            @RequestParam Long ownerId,
            @RequestParam Long roomId,
            @RequestParam String tenantName) {

        try {
            Optional<NotificationToken> tokenOpt = tokenRepository.findByUserId(ownerId);
            if (tokenOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, null, "Owner has no registered token"));
            }

            Room room = roomRepository.findById(roomId).orElse(null);

            Map<String, String> data = new HashMap<>();
            data.put("type", "booking_request");
            data.put("roomId", String.valueOf(roomId));
            data.put("tenantName", tenantName);

            fcmService.sendNotificationToUser(
                    tokenOpt.get().getFcmToken(),
                    "New Booking Request!",
                    tenantName + " wants to book " + (room != null ? room.getTitle() : "your room"),
                    data
            );

            return ResponseEntity.ok(new ApiResponse<>(true, null, "Booking notification sent"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to send notification: " + e.getMessage()));
        }
    }

    // Send booking status update to tenant
    @PostMapping("/send-booking-status")
    public ResponseEntity<ApiResponse<Void>> sendBookingStatusNotification(
            @RequestParam Long tenantId,
            @RequestParam Long roomId,
            @RequestParam String status,
            @RequestParam String roomTitle) {

        try {
            Optional<NotificationToken> tokenOpt = tokenRepository.findByUserId(tenantId);
            if (tokenOpt.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse<>(true, null, "User has no token"));
            }

            String title;
            String body;
            String type;

            switch (status.toUpperCase()) {
                case "ACCEPTED":
                    title = "Booking Accepted!";
                    body = "Your booking for " + roomTitle + " has been accepted!";
                    type = "booking_accepted";
                    break;
                case "REJECTED":
                    title = "Booking Rejected";
                    body = "Your booking for " + roomTitle + " has been rejected.";
                    type = "booking_rejected";
                    break;
                case "CANCELLED":
                    title = "Booking Cancelled";
                    body = "Your booking for " + roomTitle + " has been cancelled.";
                    type = "booking_cancelled";
                    break;
                default:
                    title = "Booking Update";
                    body = "Your booking for " + roomTitle + " status: " + status;
                    type = "booking_update";
                    break;
            }

            Map<String, String> data = new HashMap<>();
            data.put("type", type);
            data.put("roomId", String.valueOf(roomId));
            data.put("status", status);

            fcmService.sendNotificationToUser(tokenOpt.get().getFcmToken(), title, body, data);

            return ResponseEntity.ok(new ApiResponse<>(true, null, "Status notification sent"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to send notification: " + e.getMessage()));
        }
    }
}