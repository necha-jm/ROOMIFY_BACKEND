package com.ROOMIFY.Roomify.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    private String phone;

    private String businessName;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private boolean emailVerified;
    private String firebaseUid;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime lastLoginAt;

    private String passwordResetToken;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime resetTokenExpiry;

    @OneToMany(mappedBy = "user")
    private List<Booking> bookings;

    public User() {}

    public User(String name, String email, String password, UserRole role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.emailVerified = false;
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = LocalDateTime.now();
    }

    // Helper method to convert from long to LocalDateTime if needed
    public void setCreatedAtFromLong(long timestamp) {
        this.createdAt = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC);
    }

    public void setLastLoginAtFromLong(long timestamp) {
        this.lastLoginAt = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC);
    }
}