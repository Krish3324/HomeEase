package com.homeease.homeease.controller;

import com.homeease.homeease.model.Booking;
import com.homeease.homeease.model.Payment;
import com.homeease.homeease.model.User;
import com.homeease.homeease.repository.BookingRepository;
import com.homeease.homeease.repository.UserRepository;
import com.homeease.homeease.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    // ─────────────────────────────────────────
    // STEP 1: Create Razorpay order (Card / UPI)
    // ─────────────────────────────────────────
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestParam Long bookingId,
            Principal principal) {

        if (principal == null) return ResponseEntity.status(401).build();

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(404).build();

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return ResponseEntity.status(404).build();

        if (!booking.getUser().getId().equals(user.getId()))
            return ResponseEntity.status(403).build();

        if (booking.getStatus() == Booking.BookingStatus.PENDING || booking.getStatus() == Booking.BookingStatus.CANCELLED)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Payment can only be made once the worker uploads proof or the job is marked completed."));

        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Payment is not available until work proof is uploaded or the booking is completed."));

        if (booking.getStatus() == Booking.BookingStatus.IN_PROGRESS && booking.getWorkProofs().isEmpty())
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please wait for the worker to upload work proof before paying."));

        Optional<Payment> existing = paymentService.getPaymentForBooking(booking);
        if (existing.isPresent() && existing.get().getStatus() == Payment.PaymentStatus.SUCCESS)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "This booking has already been paid."));

        double amount = paymentService.calculateAmount(booking);
        if (amount < 1.0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Calculated charge is below 1.0. Razorpay payments require an amount >= 1.0."));
        }

        try {
            Payment payment = paymentService.createOrder(booking, user);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId",       payment.getRazorpayOrderId());
            response.put("amount",        (int)(payment.getAmount() * 100));
            response.put("amountDisplay", payment.getAmount());
            response.put("currency",      "INR");
            response.put("keyId",         paymentService.getKeyId());
            response.put("userName",      user.getName());
            response.put("userEmail",     user.getEmail());
            response.put("userMobile",    user.getMobile());
            response.put("workerName",    booking.getWorker().getName());
            response.put("bookingId",     booking.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create order: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // STEP 2: Verify Razorpay payment signature
    // ─────────────────────────────────────────
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestParam String razorpayOrderId,
            @RequestParam String razorpayPaymentId,
            @RequestParam String razorpaySignature,
            Principal principal) {

        if (principal == null) return ResponseEntity.status(401).build();

        boolean success = paymentService.verifyAndComplete(
                razorpayOrderId, razorpayPaymentId, razorpaySignature);

        if (success) {
            return ResponseEntity.ok(Map.of("status", "success",
                    "message", "Payment verified successfully!"));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "failed",
                            "message", "Signature verification failed. Contact support."));
        }
    }

    // ─────────────────────────────────────────
    // STEP 3: Handle payment failure
    // ─────────────────────────────────────────
    @PostMapping("/failed")
    public ResponseEntity<?> paymentFailed(@RequestParam String razorpayOrderId) {
        paymentService.markFailed(razorpayOrderId);
        return ResponseEntity.ok(Map.of("status", "recorded"));
    }

    // ─────────────────────────────────────────
    // STEP 4: Cash on Delivery confirmation
    // ─────────────────────────────────────────
    @PostMapping("/cod")
    public ResponseEntity<?> cashOnDelivery(
            @RequestParam Long bookingId,
            Principal principal) {

        if (principal == null) return ResponseEntity.status(401).build();

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(404).build();

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return ResponseEntity.status(404).build();

        if (!booking.getUser().getId().equals(user.getId()))
            return ResponseEntity.status(403).build();

        if (booking.getStatus() == Booking.BookingStatus.PENDING || booking.getStatus() == Booking.BookingStatus.CANCELLED)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "COD payment can only be made after the worker uploads proof or the booking is completed."));

        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "COD is not available until work proof is uploaded or the booking is completed."));

        if (booking.getStatus() == Booking.BookingStatus.IN_PROGRESS && booking.getWorkProofs().isEmpty())
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please wait for the worker to upload work proof before selecting COD."));

        // Check already paid
        Optional<Payment> existing = paymentService.getPaymentForBooking(booking);
        if (existing.isPresent() && existing.get().getStatus() == Payment.PaymentStatus.SUCCESS)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Payment already completed."));

        // Record COD as a SUCCESS payment (worker will collect cash)
        paymentService.recordCOD(booking, user);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Cash on Delivery confirmed successfully!"));
    }

    // ─────────────────────────────────────────
    // Check payment status for a booking
    // ─────────────────────────────────────────
    @GetMapping("/status")
    public ResponseEntity<?> paymentStatus(
            @RequestParam Long bookingId,
            Principal principal) {

        if (principal == null) return ResponseEntity.status(401).build();

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return ResponseEntity.status(404).build();

        Optional<Payment> payment = paymentService.getPaymentForBooking(booking);

        Map<String, Object> resp = new HashMap<>();
        if (payment.isPresent()) {
            resp.put("exists",     true);
            resp.put("status",     payment.get().getStatus().name());
            resp.put("amount",     payment.get().getAmount());
            resp.put("paymentId",  payment.get().getRazorpayPaymentId());
            resp.put("mode",       payment.get().getPaymentMode());
        } else {
            resp.put("exists",  false);
            resp.put("status",  "NOT_PAID");
            resp.put("amount",  paymentService.calculateAmount(booking));
        }
        return ResponseEntity.ok(resp);
    }
}