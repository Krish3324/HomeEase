package com.homeease.homeease.controller;

import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.repository.UserRepository;
import com.homeease.homeease.repository.WorkerRepository;
import com.homeease.homeease.service.QuotationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quotations")
@CrossOrigin(origins = "*")
public class QuotationController {

    @Autowired
    private QuotationService quotationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private com.homeease.homeease.repository.BookingRepository bookingRepository;

    // ===== WORKER ENDPOINTS =====

    /**
     * Get all quotation notifications for logged-in worker
     */
    @GetMapping("/worker/notifications")
    public ResponseEntity<?> getWorkerQuotationNotifications(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            Worker worker = workerRepository.findByEmail(principal.getName())
                    .orElse(null);
            if (worker == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Worker not found"));
            }

            List<Map<String, Object>> notifications = quotationService.getWorkerQuotationNotifications(worker.getId());
            long unreadCount = quotationService.getUnreadWorkerNotificationsCount(worker.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("notifications", notifications);
            response.put("unreadCount", unreadCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Mark a quotation notification as read (worker)
     */
    @PostMapping("/worker/mark-read/{notificationId}")
    public ResponseEntity<?> markWorkerNotificationAsRead(
            @PathVariable Long notificationId,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            quotationService.markAsRead(notificationId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Marked as read"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Mark all quotation notifications as read (worker)
     */
    @PostMapping("/worker/mark-all-read")
    public ResponseEntity<?> markAllWorkerNotificationsAsRead(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            Worker worker = workerRepository.findByEmail(principal.getName())
                    .orElse(null);
            if (worker == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Worker not found"));
            }

            quotationService.markAllWorkerNotificationsAsRead(worker.getId());
            return ResponseEntity.ok(Map.of("status", "success", "message", "All notifications marked as read"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Accept a quotation request (worker accepts the request, ready to submit
     * quote)
     */
    @PostMapping("/worker/accept-request/{requestId}")
    public ResponseEntity<?> acceptQuotationRequest(
            @PathVariable Long requestId,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            Worker worker = workerRepository.findByEmail(principal.getName())
                    .orElse(null);
            if (worker == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Worker not found"));
            }

            // This endpoint is mainly informational - worker will submit quote via
            // /submit-quote
            return ResponseEntity
                    .ok(Map.of("status", "success", "message", "Request accepted. You can now submit your quote."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Submit a quote from a worker for a request
     */
    @PostMapping("/worker/submit-quote")
    public ResponseEntity<?> submitQuote(
            @RequestParam Long requestId,
            @RequestParam Double amount,
            @RequestParam String message,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
            if (worker == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Worker not found"));
            }

            boolean hasActiveBooking = bookingRepository.existsByWorkerAndStatusIn(worker,
                List.of(com.homeease.homeease.model.Booking.BookingStatus.PENDING, com.homeease.homeease.model.Booking.BookingStatus.CONFIRMED, com.homeease.homeease.model.Booking.BookingStatus.IN_PROGRESS));
            if (hasActiveBooking) {
                return ResponseEntity.status(400).body(Map.of("status", "error", "message", "You cannot submit a quote because you already have an active ongoing booking."));
            }

            var notification = quotationService.submitQuote(requestId, worker.getId(), amount, message);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Quote submitted successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ===== USER ENDPOINTS =====

    /**
     * Get all quotation notifications for logged-in user (responses to their
     * requests)
     */
    @GetMapping("/user/notifications")
    public ResponseEntity<?> getUserQuotationNotifications(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"));
            }

            List<Map<String, Object>> notifications = quotationService.getUserQuotationNotifications(user.getId());
            long unreadCount = quotationService.getUnreadUserNotificationsCount(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("notifications", notifications);
            response.put("unreadCount", unreadCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get quotation history for user (all their quotation requests with responses)
     */
    @GetMapping("/user/history")
    public ResponseEntity<?> getUserQuotationHistory(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"));
            }

            List<Map<String, Object>> history = quotationService.getUserQuotationHistory(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("history", history);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Mark a quotation notification as read (user)
     */
    @PostMapping("/user/mark-read/{notificationId}")
    public ResponseEntity<?> markUserNotificationAsRead(
            @PathVariable Long notificationId,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            quotationService.markAsRead(notificationId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Marked as read"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Mark all quotation notifications as read (user)
     */
    @PostMapping("/user/mark-all-read")
    public ResponseEntity<?> markAllUserNotificationsAsRead(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"));
            }

            quotationService.markAllUserNotificationsAsRead(user.getId());
            return ResponseEntity.ok(Map.of("status", "success", "message", "All notifications marked as read"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Accept a quote (user accepts worker's quote)
     */
    @PostMapping("/user/accept-quote/{quoteId}")
    public ResponseEntity<?> acceptQuote(
            @PathVariable Long quoteId,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"));
            }

            var booking = quotationService.acceptQuote(quoteId, user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Quote accepted! Booking created.");
            response.put("bookingId", booking.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Reject a quote (user rejects worker's quote)
     */
    @PostMapping("/user/reject-quote/{quoteId}")
    public ResponseEntity<?> rejectQuote(
            @PathVariable Long quoteId,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"));
            }

            quotationService.rejectQuote(quoteId, user.getId());

            return ResponseEntity.ok(Map.of("status", "success", "message", "Quote rejected"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Cancel a quotation request (user cancels their request)
     */
    @PostMapping("/user/cancel-request/{requestId}")
    public ResponseEntity<?> cancelRequest(
            @PathVariable Long requestId,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"));
            }

            quotationService.cancelRequest(requestId, user.getId());

            return ResponseEntity.ok(Map.of("status", "success", "message", "Request cancelled"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get unread quotation notifications count for user
     */
    @GetMapping("/user/unread-count")
    public ResponseEntity<?> getUserUnreadCount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"));
            }

            long unreadCount = quotationService.getUnreadUserNotificationsCount(user.getId());

            return ResponseEntity.ok(Map.of("status", "success", "unreadCount", unreadCount));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get unread quotation notifications count for worker
     */
    @GetMapping("/worker/unread-count")
    public ResponseEntity<?> getWorkerUnreadCount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            Worker worker = workerRepository.findByEmail(principal.getName())
                    .orElse(null);
            if (worker == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Worker not found"));
            }

            long unreadCount = quotationService.getUnreadWorkerNotificationsCount(worker.getId());

            return ResponseEntity.ok(Map.of("status", "success", "unreadCount", unreadCount));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Cancel/Skip a quotation notification (worker declines request)
     */
    @PostMapping("/worker/cancel-notification/{notificationId}")
    public ResponseEntity<?> cancelNotification(
            @PathVariable Long notificationId,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        try {
            quotationService.markAsRead(notificationId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Request skipped"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
