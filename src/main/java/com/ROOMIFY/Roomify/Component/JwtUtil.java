package com.ROOMIFY.Roomify.Component;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration; // default 1 day

    private SecretKey key;

    @PostConstruct
    public void init() {
        // Ensure secret is at least 64 bytes for HS512
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            throw new RuntimeException("JWT secret must be at least 64 characters for HS512. Current length: " + keyBytes.length);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);

        System.out.println("=== JWT initialized successfully ===");
        System.out.println("Secret length: " + keyBytes.length + " bytes (>=64 required for HS512)");
        System.out.println("Algorithm: HS512");
        System.out.println("Expiration: " + expiration + " ms (" + (expiration / 1000 / 60 / 60) + " hours)");
    }

    // Generate JWT token
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS512) // Use HS512
                .compact();
    }

    // Extract email/username from token
    public String extractEmail(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token) // parses & verifies signature
                .getBody()
                .getSubject();
    }

    // Validate token (expiration & signature)
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token); // will throw exception if invalid
            return true;
        } catch (JwtException e) {
            System.out.println("Token validation failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }
}