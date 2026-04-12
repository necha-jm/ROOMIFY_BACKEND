package com.ROOMIFY.Roomify.controller;

import com.ROOMIFY.Roomify.Component.JwtUtil;
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

        Map<String, Object> response = new HashMap<>();

        try {

            // 1. Get Firebase ID token from request
            String idTokenString = request.get("idToken");

            if (idTokenString == null || idTokenString.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "message", "Missing Firebase ID token")
                );
            }

            System.out.println("Received Firebase token length: " + idTokenString.length());

            // 2. VERIFY TOKEN USING FIREBASE (NOT GOOGLE VERIFIER)
            FirebaseToken decodedToken;

            try {
                decodedToken = FirebaseAuth.getInstance().verifyIdToken(idTokenString);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "message", "Invalid Firebase token")
                );
            }

            // 3. Extract user info from Firebase token
            String email = decodedToken.getEmail();
            String uid = decodedToken.getUid();
            String name = decodedToken.getName();

            System.out.println("Firebase user: " + email);

            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "message", "Email not found in token")
                );
            }

            // 4. Find or create user
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name != null ? name : "Firebase User");
                newUser.setRole(UserRole.USER); // default role
                newUser.setCreatedAt(LocalDateTime.now());
                newUser.setEmailVerified(true);
                return userRepository.save(newUser);
            });

            // 5. Update last login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // 6. Generate JWT (your own system token)
            String jwtToken = jwtUtil.generateToken(user.getEmail());

            // 7. Response
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("email", user.getEmail());
            userData.put("name", user.getName());
            userData.put("role", user.getRole().toString().toLowerCase());

            response.put("success", true);
            response.put("token", jwtToken);
            response.put("user", userData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of(
                            "success", false,
                            "message", "Server error: " + e.getMessage()
                    )
            );
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