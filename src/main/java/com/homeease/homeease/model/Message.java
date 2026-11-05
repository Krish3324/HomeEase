package com.homeease.homeease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender; // Can be User or Worker (polymorphic - we'll handle this with a type field)

    @Column(name = "sender_type", nullable = false)
    private String senderType; // "USER" or "WORKER"

    @Column(length = 1000, nullable = false)
    private String messageText;

    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean isRead = false;

    // ===== CONSTRUCTORS =====
    public Message() {
    }

    public Message(Booking booking, User sender, String senderType, String messageText) {
        this.booking = booking;
        this.sender = sender;
        this.senderType = senderType;
        this.messageText = messageText;
        this.sentAt = LocalDateTime.now();
    }

    // ===== GETTERS & SETTERS =====
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
