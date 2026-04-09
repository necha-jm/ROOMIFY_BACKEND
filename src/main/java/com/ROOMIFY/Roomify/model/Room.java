package com.ROOMIFY.Roomify.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Getter
@Setter
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Column(name = "property_type")
    private String propertyType;

    private double price;
    private double latitude;
    private double longitude;
    private String address;

    @Column(name = "posted_by")
    private Long postedBy; // Maps to BIGINT, stores user ID

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "rooms_count")
    private int roomsCount;

    @Column(name = "bathrooms_count")
    private int bathroomsCount;

    private double area;
    private String status;

    @Column(name = "is_available")
    private boolean isAvailable;

    @Column(name = "bookings_count")
    private int bookingsCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "has_video")
    private boolean hasVideo;

    @Column(name = "has_contract")
    private boolean hasContract;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "contract_url")
    private String contractUrl;

    @Column(name = "image_count")
    private int imageCount;

    @ElementCollection
    @CollectionTable(name = "room_amenities", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "room_rules", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "rule")
    private List<String> rules = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "room_images", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "image")
    private List<String> images = new ArrayList<>();

    public Room() {
        this.isAvailable = true;
        this.status = "active";
        this.bookingsCount = 0;
        this.createdAt = LocalDateTime.now();
        this.roomsCount = 1;
        this.bathroomsCount = 1;
        this.imageCount = 0;
        this.hasVideo = false;
        this.hasContract = false;
    }
}