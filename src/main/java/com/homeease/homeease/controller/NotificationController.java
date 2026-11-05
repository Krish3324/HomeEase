package com.homeease.homeease.controller;

import com.homeease.homeease.model.Notification;
import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.repository.NotificationRepository;
import com.homeease.homeease.repository.UserRepository;
import com.homeease.homeease.repository.WorkerRepository;
import com.homeease.homeease.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private NotificationService notificationService;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM, hh:mm a");

    // ─────────────────────────────────────────
    // GET notifications for logged-in user
    // ─────────────────────────────────────────
    @GetMapping("/user")
    public ResponseEntity<?> getUserNotifications(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(404).build();

        List<Notification> notifications =
                notificationRepository.findByUserOrderByCreatedAtDesc(user);

        List<Map<String, Object>> result = notifications.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("type", n.getType().name());
            map.put("message", n.getMessage());
            map.put("otp", n.getOtp());
            map.put("read", n.isRead());
            map.put("time", n.getCreatedAt() != null ? n.getCreatedAt().format(FMT) : "");
            map.put("bookingId", n.getBooking() != null ? n.getBooking().getId() : null);
            return map;
        }).collect(Collectors.toList());

        long unread = notificationRepository.countByUserAndReadFalse(user);

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", result);
        response.put("unreadCount", unread);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────
    // GET notifications for logged-in worker
    // ─────────────────────────────────────────
    @GetMapping("/worker")
    public ResponseEntity<?> getWorkerNotifications(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null) return ResponseEntity.status(404).build();

        List<Notification> notifications =
                notificationRepository.findByWorkerOrderByCreatedAtDesc(worker);

        List<Map<String, Object>> result = notifications.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("type", n.getType().name());
            map.put("message", n.getMessage());
            map.put("read", n.isRead());
            map.put("time", n.getCreatedAt() != null ? n.getCreatedAt().format(FMT) : "");
            map.put("bookingId", n.getBooking() != null ? n.getBooking().getId() : null);
            return map;
        }).collect(Collectors.toList());

        long unread = notificationRepository.countByWorkerAndReadFalse(worker);

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", result);
        response.put("unreadCount", unread);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────
    // Mark all notifications read (user)
    // ─────────────────────────────────────────
    @PostMapping("/user/mark-read")
    public ResponseEntity<?> markUserRead(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(404).build();
        notificationService.markAllReadForUser(user);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ─────────────────────────────────────────
    // Mark all notifications read (worker)
    // ─────────────────────────────────────────
    @PostMapping("/worker/mark-read")
    public ResponseEntity<?> markWorkerRead(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null) return ResponseEntity.status(404).build();
        notificationService.markAllReadForWorker(worker);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}