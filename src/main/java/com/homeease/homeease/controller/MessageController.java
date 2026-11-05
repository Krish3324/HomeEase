package com.homeease.homeease.controller;

import com.homeease.homeease.model.Booking;
import com.homeease.homeease.model.Message;
import com.homeease.homeease.model.Role;
import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.repository.BookingRepository;
import com.homeease.homeease.repository.MessageRepository;
import com.homeease.homeease.repository.UserRepository;
import com.homeease.homeease.repository.WorkerRepository;
import com.homeease.homeease.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private MessageRepository messageRepository;

    // ═══════════════════════════════════════════════════════════════
    // SEND MESSAGE
    // ═══════════════════════════════════════════════════════════════
    @PostMapping("/send")
    @ResponseBody
    public Map<String, Object> sendMessage(
            @RequestParam Long bookingId,
            @RequestParam String messageText,
            Principal principal) {

        if (principal == null || messageText.isEmpty()) {
            return Map.of("status", "error", "message", "Invalid request");
        }

        // Get booking
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return Map.of("status", "error", "message", "Booking not found");
        }

        // Check if messaging is allowed (only for CONFIRMED bookings)
        if (!messageService.isMessagingAllowed(booking)) {
            return Map.of("status", "error", "message", "Messaging not allowed for this booking");
        }

        // Determine sender type and get sender
        User sender = null;
        String senderType = null;

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);

        if (user != null) {
            sender = user;
            if (user.getRole() == Role.ADMIN) {
                senderType = "ADMIN";
            } else {
                senderType = "USER";
            }
        } else if (worker != null) {
            // Use the booking's user (the customer) as the persistent User reference.
            // This satisfies the database foreign key and NOT NULL constraints.
            // The senderType "WORKER" will correctly distinguish it as sent by the worker.
            sender = booking.getUser();
            senderType = "WORKER";
        } else {
            return Map.of("status", "error", "message", "Sender not found");
        }

        // Send message
        Message message = messageService.sendMessage(booking, sender, senderType, messageText);
        if (message == null) {
            return Map.of("status", "error", "message", "Cannot send message at this time");
        }

        return Map.of(
                "status", "success",
                "messageId", message.getId(),
                "sentAt", message.getSentAt().toString(),
                "senderType", message.getSenderType()
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // GET ALL MESSAGES FOR A BOOKING
    // ═══════════════════════════════════════════════════════════════
    @GetMapping("/booking/{bookingId}")
    @ResponseBody
    public Map<String, Object> getMessagesForBooking(
            @PathVariable Long bookingId,
            Principal principal) {

        if (principal == null) {
            return Map.of("status", "error", "message", "Unauthorized");
        }

        // Get booking
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return Map.of("status", "error", "message", "Booking not found");
        }

        // Mark messages as read
        messageService.markMessagesAsRead(booking);

        // Get all messages
        List<Message> messages = messageService.getMessagesForBooking(booking);

        // Convert to response format
        List<Map<String, Object>> messageList = messages.stream()
                .map(msg -> {
                    Map<String, Object> msgMap = new HashMap<>();
                    msgMap.put("id", msg.getId());
                    String senderName;
                    if ("WORKER".equalsIgnoreCase(msg.getSenderType())) {
                        senderName = msg.getBooking().getWorker().getName();
                    } else if ("ADMIN".equalsIgnoreCase(msg.getSenderType())) {
                        senderName = msg.getSender() != null ? msg.getSender().getName() : "Admin";
                    } else {
                        senderName = msg.getSender() != null ? msg.getSender().getName() : "User";
                    }
                    msgMap.put("senderName", senderName);
                    msgMap.put("senderType", msg.getSenderType());
                    msgMap.put("messageText", msg.getMessageText());
                    msgMap.put("sentAt", msg.getSentAt().toString());
                    msgMap.put("isRead", msg.isRead());
                    return msgMap;
                })
                .toList();

        return Map.of(
                "status", "success",
                "messages", messageList,
                "canMessage", messageService.isMessagingAllowed(booking)
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // GET MESSAGE COUNT FOR A BOOKING
    // ═══════════════════════════════════════════════════════════════
    @GetMapping("/count/{bookingId}")
    @ResponseBody
    public Map<String, Object> getUnreadMessageCount(@PathVariable Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return Map.of("status", "error", "message", "Booking not found");
        }

        long unreadCount = messageService.getUnreadMessageCount(booking);
        return Map.of(
                "status", "success",
                "unreadCount", unreadCount
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // CHECK IF MESSAGING IS ALLOWED
    // ═══════════════════════════════════════════════════════════════
    @GetMapping("/allowed/{bookingId}")
    @ResponseBody
    public Map<String, Object> isMessagingAllowed(@PathVariable Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return Map.of("status", "error", "message", "Booking not found");
        }

        return Map.of(
                "status", "success",
                "canMessage", messageService.isMessagingAllowed(booking),
                "bookingStatus", booking.getStatus().toString()
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // GET TOTAL UNREAD MESSAGES FOR CURRENT USER
    // ═══════════════════════════════════════════════════════════════
    @GetMapping("/unread/count")
    @ResponseBody
    public Map<String, Object> getCurrentUserUnreadCount(Principal principal) {
        if (principal == null) {
            return Map.of("status", "error", "message", "Unauthorized");
        }

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user != null) {
            if (user.getRole() == Role.ADMIN) {
                long unreadCount = messageService.getTotalUnreadCount();
                return Map.of("status", "success", "unreadCount", unreadCount);
            }

            long unreadCount = messageRepository.countByBooking_UserAndIsReadFalse(user);
            return Map.of("status", "success", "unreadCount", unreadCount);
        }

        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker != null) {
            long unreadCount = messageRepository.countByBooking_WorkerAndIsReadFalse(worker);
            return Map.of("status", "success", "unreadCount", unreadCount);
        }

        return Map.of("status", "success", "unreadCount", 0);
    }
}
