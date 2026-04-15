package com.ROOMIFY.Roomify.controller;

import com.ROOMIFY.Roomify.dto.ApiResponse;
import com.ROOMIFY.Roomify.model.Room;
import com.ROOMIFY.Roomify.repository.RoomRepository;
import com.ROOMIFY.Roomify.service.FCMService;
import com.ROOMIFY.Roomify.service.RoomNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    @Autowired
    private RoomRepository repo;

    @Autowired
    private NotificationController notificationController;  // ADD THIS - Fix missing injection

    @Autowired
    private RoomNotifier notifier;

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    // Create a new room
    @PostMapping
    @Transactional
    public ResponseEntity<?> addRoom(@RequestBody Room room) {
        try {

            // 🔐 SAFETY CHECK
            if (room == null) {
                return ResponseEntity.badRequest().body("Room is null");
            }

            // ================= DEFAULT VALUES =================
            if (room.getCreatedAt() == null) {
                room.setCreatedAt(LocalDateTime.now());
            }

            if (room.getStatus() == null || room.getStatus().isEmpty()) {
                room.setStatus("active");
            }

            if (room.getRoomsCount() <= 0) {
                room.setRoomsCount(1);
            }

            if (room.getBathroomsCount() <= 0) {
                room.setBathroomsCount(1);
            }

            if (room.getAmenities() == null) {
                room.setAmenities(new ArrayList<>());
            }

            if (room.getRules() == null) {
                room.setRules(new ArrayList<>());
            }

            if (room.getImages() == null) {
                room.setImages(new ArrayList<>());
            }

            if (room.getPostedBy() == null) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("postedBy is required");
            }

            room.setAvailable(true);

            // ================= SAVE =================
            Room saved = repo.save(room);

            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace(); // 🔥 THIS WILL SHOW REAL ERROR IN LOGCAT
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    // Update room
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room roomDetails) {
        try {
            Room room = repo.findById(id).orElse(null);
            if (room == null) return ResponseEntity.notFound().build();

            // Update fields
            room.setTitle(roomDetails.getTitle());
            room.setDescription(roomDetails.getDescription());
            room.setPrice(roomDetails.getPrice());
            room.setLatitude(roomDetails.getLatitude());
            room.setLongitude(roomDetails.getLongitude());
            room.setAddress(roomDetails.getAddress());
            room.setPropertyType(roomDetails.getPropertyType());
            room.setContactPhone(roomDetails.getContactPhone());
            room.setContactEmail(roomDetails.getContactEmail());
            room.setOwnerName(roomDetails.getOwnerName());
            room.setAmenities(roomDetails.getAmenities());
            room.setRules(roomDetails.getRules());
            room.setRoomsCount(roomDetails.getRoomsCount());
            room.setBathroomsCount(roomDetails.getBathroomsCount());
            room.setArea(roomDetails.getArea());
            room.setAvailable(roomDetails.isAvailable());
            room.setHasVideo(roomDetails.isHasVideo());
            room.setHasContract(roomDetails.isHasContract());
            room.setVideoUrl(roomDetails.getVideoUrl());
            room.setContractUrl(roomDetails.getContractUrl());
            room.setImages(roomDetails.getImages());
            room.setImageCount(roomDetails.getImages() != null ? roomDetails.getImages().size() : 0);
            room.setStatus(roomDetails.getStatus());

            Room updated = repo.save(room);
            notifier.notifyRoomUpdate(updated);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Delete room
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        try {
            Room room = repo.findById(id).orElse(null);
            if (room == null) return ResponseEntity.notFound().build();

            repo.deleteById(id);
            notifier.notifyRoomUpdate(room);
            return ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting room");
        }
    }

    // Get room by ID
    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        Room room = repo.findById(id).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(room);
    }

    // Get all rooms
    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(repo.findAll());
    }

    // Get all rooms posted by a specific user
    @GetMapping("/owner/{postedBy}")
    public ResponseEntity<List<Room>> getRoomsByOwner(@PathVariable Long postedBy) {
        return ResponseEntity.ok(repo.findByPostedBy(postedBy));
    }

    // Get all available rooms
    @GetMapping("/available")
    public ResponseEntity<List<Room>> getAvailableRooms() {
        return ResponseEntity.ok(repo.findByIsAvailableTrue());
    }

    // Get rooms by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Room>> getRoomsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(repo.findByStatus(RoomRepository.RoomStatus.valueOf(status)));
    }

    // ==================== MEDIA UPLOAD ENDPOINTS ====================

    // Upload images for a room
    @PostMapping("/{roomId}/images")
    @Transactional
    public ResponseEntity<ApiResponse<List<String>>> uploadRoomImages(
            @PathVariable Long roomId,
            @RequestParam("images") MultipartFile[] files) {

        try {
            Room room = repo.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));

            List<String> imageUrls = new ArrayList<>();

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir, "rooms", String.valueOf(roomId), "images");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            for (MultipartFile file : files) {
                String originalFileName = file.getOriginalFilename();
                String fileExtension = "";
                if (originalFileName != null && originalFileName.contains(".")) {
                    fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                }
                String fileName = UUID.randomUUID().toString() + fileExtension;
                Path filePath = uploadPath.resolve(fileName);
                Files.write(filePath, file.getBytes());

                String fileUrl = "/uploads/rooms/" + roomId + "/images/" + fileName;
                imageUrls.add(fileUrl);
            }

            // Update room with image URLs
            List<String> existingImages = room.getImages();
            if (existingImages == null) {
                existingImages = new ArrayList<>();
            }
            existingImages.addAll(imageUrls);
            room.setImages(existingImages);
            room.setImageCount(existingImages.size());
            repo.save(room);

            return ResponseEntity.ok(new ApiResponse<>(true, imageUrls, "Images uploaded successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to upload images: " + e.getMessage()));
        }
    }

    // Upload video for a room
    @PostMapping("/{roomId}/video")
    @Transactional
    public ResponseEntity<ApiResponse<String>> uploadRoomVideo(
            @PathVariable Long roomId,
            @RequestParam("video") MultipartFile file) {

        try {
            Room room = repo.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));

            // Check file size (max 50MB)
            if (file.getSize() > 50 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(new ApiResponse<>(false, null, "Video file too large. Max size is 50MB"));
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir, "rooms", String.valueOf(roomId), "videos");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            String videoUrl = "/uploads/rooms/" + roomId + "/videos/" + fileName;

            // Update room with video URL
            room.setVideoUrl(videoUrl);
            room.setHasVideo(true);
            repo.save(room);

            return ResponseEntity.ok(new ApiResponse<>(true, videoUrl, "Video uploaded successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to upload video: " + e.getMessage()));
        }
    }

    // Upload contract for a room
    @PostMapping("/{roomId}/contract")
    @Transactional
    public ResponseEntity<ApiResponse<String>> uploadRoomContract(
            @PathVariable Long roomId,
            @RequestParam("contract") MultipartFile file) {

        try {
            Room room = repo.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));

            // Check file size (max 10MB for PDF)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(new ApiResponse<>(false, null, "Contract file too large. Max size is 10MB"));
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir, "rooms", String.valueOf(roomId), "contracts");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            String contractUrl = "/uploads/rooms/" + roomId + "/contracts/" + fileName;

            // Update room with contract URL
            room.setContractUrl(contractUrl);
            room.setHasContract(true);
            repo.save(room);

            return ResponseEntity.ok(new ApiResponse<>(true, contractUrl, "Contract uploaded successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Failed to upload contract: " + e.getMessage()));
        }
    }
}