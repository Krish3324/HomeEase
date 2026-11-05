package com.homeease.homeease.service;

import com.homeease.homeease.model.Booking;
import com.homeease.homeease.model.Payment;
import com.homeease.homeease.model.User;
import com.homeease.homeease.repository.PaymentRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PaymentService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private com.homeease.homeease.repository.BookingRepository bookingRepository;

    // ─────────────────────────────────────────
    // Calculate average charge with rebooking discount (20% if within 30 days)
    // ─────────────────────────────────────────
    public double calculateAmount(Booking booking) {
        double min = booking.getWorker().getMinCharge() != null ? booking.getWorker().getMinCharge() : 0;
        double max = booking.getWorker().getMaxCharge() != null ? booking.getWorker().getMaxCharge() : 0;
        double baseAmount = Math.round((min + max) / 2.0 * 100.0) / 100.0;

        // Check for rebooking discount
        var lastBookingOpt = bookingRepository.findFirstByUserAndWorkerAndStatusOrderByCreatedAtDesc(
                booking.getUser(), booking.getWorker(), Booking.BookingStatus.COMPLETED);

        if (lastBookingOpt.isPresent()) {
            java.time.LocalDateTime lastCompletedAt = lastBookingOpt.get().getCreatedAt();
            if (lastCompletedAt.isAfter(java.time.LocalDateTime.now().minusDays(30))) {
                double discountedAmount = Math.round(baseAmount * 0.8 * 100.0) / 100.0;
                booking.setOriginalAmount(baseAmount);
                booking.setDiscountedAmount(discountedAmount);
                booking.setAppliedDiscount(20.0);
                return discountedAmount;
            }
        }

        booking.setOriginalAmount(baseAmount);
        booking.setDiscountedAmount(baseAmount);
        booking.setAppliedDiscount(0.0);
        return baseAmount;
    }

    // ─────────────────────────────────────────
    // Create Razorpay Order (Card / UPI)
    // ─────────────────────────────────────────
    public Payment createOrder(Booking booking, User user) throws RazorpayException {

        Optional<Payment> existing = paymentRepository.findByBooking(booking);
        if (existing.isPresent() && existing.get().getStatus() == Payment.PaymentStatus.SUCCESS) {
            return existing.get();
        }

        // Use stored discounted amount if available, otherwise calculate
        double amount = booking.getDiscountedAmount() != null && booking.getDiscountedAmount() > 0
            ? booking.getDiscountedAmount()
            : calculateAmount(booking);

        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", (int) (amount * 100)); // paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "booking_" + booking.getId());
        orderRequest.put("payment_capture", 1);

        Order razorpayOrder = client.orders.create(orderRequest);

        Payment payment = existing.orElse(new Payment());
        payment.setBooking(booking);
        payment.setUser(user);
        payment.setRazorpayOrderId(razorpayOrder.get("id"));
        payment.setAmount(amount);
        payment.setPaymentMode(Payment.PaymentMode.RAZORPAY);
        payment.setStatus(Payment.PaymentStatus.PENDING);

        return paymentRepository.save(payment);
    }

    // ─────────────────────────────────────────
    // Verify Razorpay Signature & Mark Success
    // ─────────────────────────────────────────
    public boolean verifyAndComplete(String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {
        try {
            String data = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = HexFormat.of().formatHex(hash);

            if (!generatedSignature.equals(razorpaySignature)) {
                return false;
            }

            Payment payment = paymentRepository
                    .findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(() -> new RuntimeException("Payment not found"));

            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setRazorpaySignature(razorpaySignature);
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─────────────────────────────────────────
    // Record COD payment (cash collected by worker)
    // ─────────────────────────────────────────
    public Payment recordCOD(Booking booking, User user) {
        Optional<Payment> existing = paymentRepository.findByBooking(booking);
        Payment payment = existing.orElse(new Payment());
        payment.setBooking(booking);
        payment.setUser(user);
        // Use stored discounted amount if available, otherwise calculate
        double amount = booking.getDiscountedAmount() != null && booking.getDiscountedAmount() > 0
            ? booking.getDiscountedAmount()
            : calculateAmount(booking);
        payment.setAmount(amount);
        payment.setPaymentMode(Payment.PaymentMode.COD);
        // COD = success immediately (cash will be paid on arrival)
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    // ─────────────────────────────────────────
    // Mark payment as FAILED
    // ─────────────────────────────────────────
    public void markFailed(String razorpayOrderId) {
        paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(p -> {
            p.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(p);
        });
    }

    // ─────────────────────────────────────────
    // Get payment for a booking
    // ─────────────────────────────────────────
    public Optional<Payment> getPaymentForBooking(Booking booking) {
        return paymentRepository.findByBooking(booking);
    }

    public String getKeyId() {
        return keyId;
    }
}