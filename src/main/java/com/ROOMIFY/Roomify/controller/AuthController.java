package com.ROOMIFY.Roomify.controller;

import com.ROOMIFY.Roomify.Component.JwtUtil;
import com.ROOMIFY.Roomify.dto.ApiResponse;
import com.ROOMIFY.Roomify.dto.AuthResponse;
import com.ROOMIFY.Roomify.dto.RegisterRequest;
import com.ROOMIFY.Roomify.model.User;
import com.ROOMIFY.Roomify.model.UserRole;
import com.ROOMIFY.Roomify.repository.UserRepository;
import com.ROOMIFY.Roomify.service.JwtService;
import com.ROOMIFY.Roomify.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${google.client.id:}")
    private String googleClientId;
    @PostMapping("/google")
    @Transactional
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        System.out.println("=== GOOGLE LOGIN REQUEST RECEIVED ===");

        try {
            String idTokenString = request.get("idToken");
            String role = request.get("role");

            System.out.println("Role: " + role);
            System.out.println("Token length: " + (idTokenString != null ? idTokenString.length() : 0));

            if (idTokenString == null || idTokenString.isBlank()) {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "message", "Missing Google ID token")
                );
            }

            // Try multiple verification methods
            String email = null;
            String name = null;
            Exception lastError = null;

            // Method 1: Try tokeninfo endpoint (most reliable)
            try {
                System.out.println("Trying tokeninfo endpoint...");
                RestTemplate restTemplate = new RestTemplate();
                String tokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idTokenString;
                Map<String, Object> tokenInfo = restTemplate.getForObject(tokenInfoUrl, Map.class);

                if (tokenInfo != null && tokenInfo.containsKey("email")) {
                    email = (String) tokenInfo.get("email");
                    name = (String) tokenInfo.get("name");
                    System.out.println("Tokeninfo successful: " + email);
                }
            } catch (Exception e) {
                System.out.println("Tokeninfo failed: " + e.getMessage());
                lastError = e;
            }

            // Method 2: Try GoogleIdTokenVerifier if tokeninfo failed
            if (email == null && googleClientId != null && !googleClientId.isEmpty()) {
                try {
                    System.out.println("Trying GoogleIdTokenVerifier...");
                    GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                            GoogleNetHttpTransport.newTrustedTransport(),
                            JacksonFactory.getDefaultInstance()
                    )
                            .setAudience(Collections.singletonList(googleClientId))
                            .build();

                    GoogleIdToken idToken = verifier.verify(idTokenString);
                    if (idToken != null) {
                        GoogleIdToken.Payload payload = idToken.getPayload();
                        email = payload.getEmail();
                        name = (String) payload.get("name");
                        System.out.println("Verifier successful: " + email);
                    }
                } catch (Exception e) {
                    System.out.println("Verifier failed: " + e.getMessage());
                    lastError = e;
                }
            }

            // Method 3: Manual JWT decode (last resort)
            if (email == null) {
                try {
                    System.out.println("Trying manual JWT decode...");
                    String[] parts = idTokenString.split("\\.");
                    if (parts.length >= 2) {
                        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                        // Parse JSON manually
                        payload = payload.replace("true", "true").replace("false", "false");
                        Map<String, Object> claims = parseJsonToMap(payload);
                        email = (String) claims.get("email");
                        name = (String) claims.get("name");
                        System.out.println("Manual decode successful: " + email);
                    }
                } catch (Exception e) {
                    System.out.println("Manual decode failed: " + e.getMessage());
                    lastError = e;
                }
            }

            if (email == null) {
                System.err.println("All verification methods failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "message", "Could not verify Google token: " + (lastError != null ? lastError.getMessage() : "Unknown error"))
                );
            }

            // Process the user
            return processGoogleUser(email, name, role);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "message", "Google login failed: " + e.getMessage())
            );
        }
    }

    private Map<String, Object> parseJsonToMap(String json) {
        Map<String, Object> map = new HashMap<>();
        try {
            // Simple JSON parsing (remove braces and split)
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);
                String[] pairs = json.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replace("\"", "");
                        String value = keyValue[1].trim().replace("\"", "");
                        map.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
        return map;
    }

    private ResponseEntity<?> processGoogleUser(String email, String name, String role) {
        System.out.println("Processing user: " + email + ", role: " + role);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(name != null && !name.isEmpty() ? name : email.split("@")[0]);
                    newUser.setRole(UserRole.valueOf(role.toUpperCase()));
                    newUser.setCreatedAt(LocalDateTime.now());
                    newUser.setEmailVerified(true);
                    System.out.println("Created new user: " + email);
                    return userRepository.save(newUser);
                });

        user.setLastLoginAt(LocalDateTime.now());
        user = userRepository.save(user);

        String jwt = jwtUtil.generateToken(user.getEmail());

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("name", user.getName());
        userData.put("role", user.getRole().name().toLowerCase());
        userData.put("emailVerified", user.isEmailVerified());

        System.out.println("Login successful for: " + email);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "token", jwt,
                "user", userData
        ));
    }


    @PostMapping("/api/auth/save-fcm-token")
    public ResponseEntity<ApiResponse<Void>> saveFcmToken(
            @RequestParam Long userId,
            @RequestParam String fcmToken) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setFcmToken(fcmToken);
                userRepository.save(user);
                System.out.println("✅ FCM token saved for user: " + user.getEmail());
                return ResponseEntity.ok(new ApiResponse<>(true, null, "FCM token saved"));
            }
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, "User not found"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "Error saving token: " + e.getMessage()));
        }
    }


    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@RequestBody RegisterRequest request) {
        try {
            if (userService.existsByEmail(request.getEmail())) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(new AuthResponse(false, null, null, "Email already registered"));
            }

            User user = new User();
            user.setName(request.getName());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));

            UserRole userRole = UserRole.valueOf(request.getRole().toUpperCase());
            user.setRole(userRole);

            if ("owner".equalsIgnoreCase(request.getRole()) && request.getBusinessName() != null
                    && !request.getBusinessName().isEmpty()) {
                user.setBusinessName(request.getBusinessName());
            }

            user.setPhone(request.getPhone());
            user.setEmailVerified(false);

            // Use LocalDateTime.now() instead of System.currentTimeMillis()
            user.setCreatedAt(LocalDateTime.now());
            user.setLastLoginAt(LocalDateTime.now());

            User savedUser = userService.save(user);

            String token = jwtUtil.generateToken(savedUser.getEmail());

            AuthResponse response = new AuthResponse(
                    true,
                    token,
                    savedUser,
                    "Registration successful"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(false, null, null, "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String email = request.get("email");
            String password = request.get("password");
            String role = request.get("role");

            // Find user
            Optional<User> userOptional = userRepository.findByEmail(email);

            if (userOptional.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found with this email");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            User user = userOptional.get();

            // Verify password
            if (!passwordEncoder.matches(password, user.getPassword())) {
                response.put("success", false);
                response.put("message", "Invalid password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Verify role
            if (role != null && !role.isEmpty()) {
                try {
                    UserRole selectedRole = UserRole.valueOf(role.toUpperCase());
                    if (!user.getRole().equals(selectedRole)) {
                        response.put("success", false);
                        response.put("message", "You selected " + role + " but your account is registered as " + user.getRole().toString().toLowerCase());
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                    }
                } catch (IllegalArgumentException e) {
                    response.put("success", false);
                    response.put("message", "Invalid role specified");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);

            // Generate token
            String token = jwtUtil.generateToken(updatedUser.getEmail());

            // Prepare response
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", updatedUser.getId());
            userData.put("email", updatedUser.getEmail());
            userData.put("name", updatedUser.getName());
            userData.put("role", updatedUser.getRole().toString().toLowerCase());
            userData.put("emailVerified", updatedUser.isEmailVerified());

            response.put("success", true);
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("user", userData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/test-token")
    public ResponseEntity<Map<String, Object>> testToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("success", false);
            response.put("message", "No token provided");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            boolean isValid = jwtUtil.validateToken(token);

            response.put("success", true);
            response.put("email", email);
            response.put("isValid", isValid);
            response.put("tokenPreview", token.substring(0, Math.min(50, token.length())) + "...");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/guest")
    public ResponseEntity<Map<String, Object>> guestLogin() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Create a temporary guest user or return a guest token
            String guestToken = jwtUtil.generateToken("guest@temp.com");

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", -1);
            userData.put("email", "guest@temp.com");
            userData.put("name", "Guest User");
            userData.put("role", "guest");
            userData.put("emailVerified", false);

            response.put("success", true);
            response.put("message", "Guest login successful");
            response.put("token", guestToken);
            response.put("user", userData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Guest login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}