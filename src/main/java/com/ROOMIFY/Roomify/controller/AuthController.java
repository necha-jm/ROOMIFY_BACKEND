package com.ROOMIFY.Roomify.controller;

import com.ROOMIFY.Roomify.Component.JwtUtil;
import com.ROOMIFY.Roomify.dto.AuthResponse;
import com.ROOMIFY.Roomify.dto.RegisterRequest;
import com.ROOMIFY.Roomify.model.User;
import com.ROOMIFY.Roomify.model.UserRole;
import com.ROOMIFY.Roomify.repository.UserRepository;
import com.ROOMIFY.Roomify.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
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
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String idTokenString = request.get("idToken");
            String selectedRole = request.get("role");

            // Verify Google token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                response.put("success", false);
                response.put("message", "Invalid Google token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String googleId = payload.getSubject();

            // Convert role to enum
            UserRole userRole = null;
            if (selectedRole != null && !selectedRole.isEmpty()) {
                try {
                    userRole = UserRole.valueOf(selectedRole.toUpperCase());
                } catch (IllegalArgumentException e) {
                    response.put("success", false);
                    response.put("message", "Invalid role: " + selectedRole);
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Check if user exists
            Optional<User> userOptional = userRepository.findByEmail(email);
            User user;
            boolean isNewUser = false;

            if (userOptional.isEmpty()) {
                // Create new user
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setRole(userRole != null ? userRole : UserRole.TENANT);
                user.setEmailVerified(true);
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                user.setFirebaseUid(googleId);
                user.setCreatedAt(LocalDateTime.now());
                user.setLastLoginAt(LocalDateTime.now());
                isNewUser = true;
            } else {
                user = userOptional.get();

                // Verify role matches if selected
                if (userRole != null && !user.getRole().equals(userRole)) {
                    response.put("success", false);
                    response.put("message", "You selected " + selectedRole + " but your account is registered as " + user.getRole().toString().toLowerCase());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                }

                // Update existing user
                user.setLastLoginAt(LocalDateTime.now());
                if (user.getFirebaseUid() == null) {
                    user.setFirebaseUid(googleId);
                }
            }

            // SINGLE SAVE operation
            User savedUser = userRepository.save(user);

            // Generate JWT token
            String token = jwtUtil.generateToken(savedUser.getEmail());

            // Prepare response
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", savedUser.getId());
            userData.put("email", savedUser.getEmail());
            userData.put("name", savedUser.getName());
            userData.put("role", savedUser.getRole().toString().toLowerCase());
            userData.put("emailVerified", savedUser.isEmailVerified());

            response.put("success", true);
            response.put("message", isNewUser ? "Account created successfully" : "Login successful");
            response.put("token", token);
            response.put("user", userData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Google login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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