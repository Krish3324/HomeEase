package com.homeease.homeease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    public enum NotificationType {
        // Booking related
        BOOKING_PLACED, // worker gets: new booking request
        BOOKING_ACCEPTED, // user gets: worker accepted + OTP
        BOOKING_DECLINED, // user gets: worker declined
        BOOKING_COMPLETED, // user gets: job completed

        // Quotation related
        QUOTATION_REQUEST, // worker gets: new quotation request from user
        QUOTATION_SUBMITTED, // user gets: worker submitted a quote
        QUOTATION_ACCEPTED, // worker gets: user accepted the quote
        QUOTATION_REJECTED, // worker gets: user rejected the quote
        REQUEST_CANCELLED // worker gets: user cancelled the request
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "worker_id")
    private Worker worker;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(length = 1000)
    private String message;

    private String otp;

    // ✅ FIX: "read" is a reserved keyword in MySQL — renamed to "is_read"
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    // ===== GETTERS & SETTERS =====
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}