package com.homeease.homeease.controller;

import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.repository.UserRepository;
import com.homeease.homeease.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    /**
     * Get current logged-in user information
     */
    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Not authenticated"));
        }

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user != null) {
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "role", "user",
                    "phoneNumber", user.getMobile()));
        }

        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker != null) {
            return ResponseEntity.ok(Map.of(
                    "id", worker.getId(),
                    "name", worker.getName(),
                    "email", worker.getEmail(),
                    "role", "worker",
                    "phoneNumber", worker.getMobile()));
        }

        return ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"));
    }

    /**
     * Get current user's email
     */
    @GetMapping("/current-user/email")
    public ResponseEntity<?> getCurrentUserEmail(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Not authenticated"));
        }

        return ResponseEntity.ok(Map.of("email", principal.getName()));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "HomeEase API is running",
                "timestamp", System.currentTimeMillis()));
    }

    /**
     * Get API version
     */
    @GetMapping("/version")
    public ResponseEntity<?> version() {
        return ResponseEntity.ok(Map.of(
                "version", "1.0.0",
                "name", "HomeEase API",
                "features", new String[] {
                        "quotation_notifications",
                        "real_time_messaging",
                        "booking_management",
                        "payment_processing",
                        "worker_ratings"
                }));
    }
}
