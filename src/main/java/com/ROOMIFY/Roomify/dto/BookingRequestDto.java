package com.ROOMIFY.Roomify.dto;

public class BookingRequestDto {
    private Long roomId;
    private Long userId;
    private String status;
    private Double totalPrice;
    private String startDate;
    private String endDate;
    private Integer numberOfGuests;
    private String specialRequests;
    private String bookingDate;

    // Constructors
    public BookingRequestDto() {}

    // Getters
    public Long getRoomId() { return roomId; }
    public Long getUserId() { return userId; }
    public String getStatus() { return status; }
    public Double getTotalPrice() { return totalPrice; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public Integer getNumberOfGuests() { return numberOfGuests; }
    public String getSpecialRequests() { return specialRequests; }
    public String getBookingDate() { return bookingDate; }

    // Setters
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setStatus(String status) { this.status = status; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public void setNumberOfGuests(Integer numberOfGuests) { this.numberOfGuests = numberOfGuests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }
}