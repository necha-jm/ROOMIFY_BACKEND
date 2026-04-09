package com.ROOMIFY.Roomify.Component;

import io.jsonwebtoken.Jwts;
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
    private Long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // Ensure secret is at least 64 characters for HS512
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

    public String generateToken(String email) {
        String token = Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, Jwts.SIG.HS512)  // Explicitly use HS512
                .compact();

        System.out.println("Generated token for: " + email);
        System.out.println("Token length: " + token.length());
        return token;
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            System.out.println("Token validated successfully");
            return true;
        } catch (Exception e) {
            System.out.println("Token validation failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }
}