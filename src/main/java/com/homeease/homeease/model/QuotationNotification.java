package com.homeease.homeease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotation_notifications")
public class QuotationNotification {

    public enum QuotationType {
        REQUEST_SENT, // worker gets: new quotation request from user
        QUOTE_SUBMITTED, // user gets: worker submitted a quote
        QUOTE_ACCEPTED, // worker gets: user accepted the quote
        QUOTE_REJECTED, // worker gets: user rejected the quote
        REQUEST_CANCELLED, // worker gets: user cancelled the request
        REQUEST_EXPIRED // both get: request expired
    }

    public enum QuotationStatus {
        PENDING, // Initial state - waiting for action
        ACCEPTED, // Quote accepted by user
        REJECTED, // Quote rejected by user
        CANCELLED, // Request cancelled by user
        COMPLETED, // Job completed
        EXPIRED // Request expired
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user who created the original service request
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The worker who is responding/receiving the request
    @ManyToOne
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    // Link to the service request
    @ManyToOne
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    // Link to the service quote (if exists)
    @ManyToOne
    @JoinColumn(name = "service_quote_id")
    private ServiceQuote serviceQuote;

    // Link to booking (if quote accepted)
    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuotationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuotationStatus status = QuotationStatus.PENDING;

    @Column(length = 1000)
    private String message;

    // For worker's quote amount
    private Double quoteAmount;

    // For tracking if user/worker has read this notification
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    // For user notifications only - if they want to mark as relevant/favorite
    private boolean isUserNotified = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

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

    public ServiceRequest getServiceRequest() {
        return serviceRequest;
    }

    public void setServiceRequest(ServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
    }

    public ServiceQuote getServiceQuote() {
        return serviceQuote;
    }

    public void setServiceQuote(ServiceQuote serviceQuote) {
        this.serviceQuote = serviceQuote;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public QuotationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(QuotationType notificationType) {
        this.notificationType = notificationType;
    }

    public QuotationStatus getStatus() {
        return status;
    }

    public void setStatus(QuotationStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Double getQuoteAmount() {
        return quoteAmount;
    }

    public void setQuoteAmount(Double quoteAmount) {
        this.quoteAmount = quoteAmount;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isUserNotified() {
        return isUserNotified;
    }

    public void setUserNotified(boolean userNotified) {
        isUserNotified = userNotified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
