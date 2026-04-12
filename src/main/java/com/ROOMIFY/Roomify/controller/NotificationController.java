package com.ROOMIFY.Roomify.controller;

import com.ROOMIFY.Roomify.dto.ApiResponse;
import com.ROOMIFY.Roomify.model.NotificationToken;
import com.ROOMIFY.Roomify.model.Room;
import com.ROOMIFY.Roomify.repository.NotificationTokenRepository;
import com.ROOMIFY.Roomify.repository.RoomRepository;
import com.ROOMIFY.Roomify.service.FCMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin("*")
public class NotificationController {

    @Autowired
    private FCMService fcmService;

    @Autowired
    private NotificationTokenRepository tokenRepository;

    @Autowired
    private RoomRepository roomRepository;

    // REGISTER TOKEN
    @PostMapping("/register-token")
    public ResponseEntity<ApiResponse<Void>> registerToken(
            @RequestParam Long userId,
            @RequestParam String fcmToken) {

        try {
            NotificationToken token = tokenRepository
                    .findByUserId(userId)
                    .orElse(new NotificationToken());

            token.setUserId(userId);
            token.setFcmToken(fcmToken);
            token.setActive(true);

            tokenRepository.save(token);

            return ResponseEntity.ok(
                    new ApiResponse<>(true, null, "Token saved")
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, null, e.getMessage()));
        }
    }

    // SEND TO USER
    @PostMapping("/send-to-user")
    public ResponseEntity<ApiResponse<Void>> sendToUser(
            @RequestParam Long userId,
            @RequestParam String title,
            @RequestParam String body) {

        try {
            NotificationToken token = tokenRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Token not found"));

            fcmService.sendNotificationToUser(
                    token.getFcmToken(),
                    title,
                    body,
                    new HashMap<>()
            );

            return ResponseEntity.ok(
                    new ApiResponse<>(true, null, "Sent")
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, null, e.getMessage()));
        }
    }

    // ROOM NOTIFICATION
    @PostMapping("/send-room-notification")
    public ResponseEntity<ApiResponse<Void>> sendRoomNotification(@RequestParam Long roomId) {

        try {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Room not found"));

            List<NotificationToken> tokens = tokenRepository.findAll();

            for (NotificationToken t : tokens) {
                fcmService.sendNotificationToUser(
                        t.getFcmToken(),
                        "New Room Available",
                        room.getTitle(),
                        Map.of("type", "new_room", "roomId", String.valueOf(roomId))
                );
            }

            return ResponseEntity.ok(
                    new ApiResponse<>(true, null, "Notifications sent")
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, null, e.getMessage()));
        }
    }
}