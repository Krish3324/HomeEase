package com.homeease.homeease.service;

import com.homeease.homeease.model.Booking;
import com.homeease.homeease.model.Message;
import com.homeease.homeease.model.User;
import com.homeease.homeease.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    // ─────────────────────────────────────────
    // Send a message
    // ─────────────────────────────────────────
    public Message sendMessage(Booking booking, User sender, String senderType, String messageText) {
        // Only allow messaging if booking is CONFIRMED, IN_PROGRESS or COMPLETED
        if (!booking.getStatus().equals(Booking.BookingStatus.CONFIRMED) && 
            !booking.getStatus().equals(Booking.BookingStatus.IN_PROGRESS) &&
            !booking.getStatus().equals(Booking.BookingStatus.COMPLETED)) {
            return null; // Messaging not allowed for pending or cancelled bookings
        }

        Message message = new Message(booking, sender, senderType, messageText);
        return messageRepository.save(message);
    }

    // ─────────────────────────────────────────
    // Get all messages for a booking
    // ─────────────────────────────────────────
    public List<Message> getMessagesForBooking(Booking booking) {
        return messageRepository.findByBookingOrderBySentAtAsc(booking);
    }

    // ─────────────────────────────────────────
    // Mark all messages in a booking as read
    // ─────────────────────────────────────────
    public void markMessagesAsRead(Booking booking) {
        List<Message> unreadMessages = messageRepository.findByBookingAndIsReadFalse(booking);
        unreadMessages.forEach(msg -> {
            msg.setRead(true);
            messageRepository.save(msg);
        });
    }

    // ─────────────────────────────────────────
    // Check if messaging is allowed for booking
    // ─────────────────────────────────────────
    public boolean isMessagingAllowed(Booking booking) {
        return booking.getStatus().equals(Booking.BookingStatus.CONFIRMED)
                || booking.getStatus().equals(Booking.BookingStatus.IN_PROGRESS)
                || booking.getStatus().equals(Booking.BookingStatus.COMPLETED);
    }

    // ─────────────────────────────────────────
    // Get unread message count for a booking
    // ─────────────────────────────────────────
    public long getUnreadMessageCount(Booking booking) {
        return messageRepository.countByBookingAndIsReadFalse(booking);
    }

    // ─────────────────────────────────────────
    // Get total unread message count across bookings
    // ─────────────────────────────────────────
    public long getTotalUnreadCount() {
        return messageRepository.countByIsReadFalse();
    }
}
