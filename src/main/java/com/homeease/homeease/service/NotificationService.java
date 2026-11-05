package com.homeease.homeease.service;

import com.homeease.homeease.model.*;
import com.homeease.homeease.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    // ─────────────────────────────────────────
    // Called after user books a worker
    // → creates a BOOKING_PLACED notification for the WORKER
    // ─────────────────────────────────────────
    public void notifyWorkerNewBooking(Booking booking, String otp) {
        Notification n = new Notification();
        n.setWorker(booking.getWorker());
        n.setBooking(booking);
        n.setOtp(otp); // Store it here too for easy lookup
        n.setType(Notification.NotificationType.BOOKING_PLACED);
        n.setMessage("📋 New booking request from <b>" + booking.getUser().getName() + "</b>"
                + " on <b>" + booking.getBookingDate() + "</b> at <b>" + booking.getBookingTime() + "</b>"
                + " — " + booking.getDescription());
        notificationRepository.save(n);
    }

    public void notifyUserBookingPlaced(Booking booking, String otp) {
        Notification n = new Notification();
        n.setUser(booking.getUser());
        n.setBooking(booking);
        n.setType(Notification.NotificationType.BOOKING_PLACED);
        n.setOtp(otp);
        n.setMessage("📅 Your booking request for <b>" + booking.getWorker().getName() + "</b> has been placed!"
                + " Your confirmation OTP is: <b class='otp-code'>" + otp + "</b>"
                + " — Please keep this safe. The worker will ask for it to complete the job.");
        notificationRepository.save(n);
    }

    // ─────────────────────────────────────────
    // Called when worker ACCEPTS a booking
    // → generates OTP, saves notification for USER, sends OTP email
    // ─────────────────────────────────────────
    public String notifyUserBookingAccepted(Booking booking) {
        String otp = generateOtp();

        Notification n = new Notification();
        n.setUser(booking.getUser());
        n.setBooking(booking);
        n.setType(Notification.NotificationType.BOOKING_ACCEPTED);
        n.setOtp(otp);
        n.setMessage("✅ <b>" + booking.getWorker().getName() + "</b> has accepted your booking!"
                + " Your confirmation OTP is: <b class='otp-code'>" + otp + "</b>"
                + " — Share this OTP with the worker when they arrive at your home.");
        notificationRepository.save(n);

        // Send OTP via email
        emailService.sendOtpEmail(
                booking.getUser().getEmail(),
                booking.getUser().getName(),
                booking.getWorker().getName(),
                otp,
                booking.getBookingDate().toString(),
                booking.getBookingTime().toString()
        );

        return otp;
    }

    // ─────────────────────────────────────────
    // Called when worker DECLINES a booking
    // → saves notification for USER
    // ─────────────────────────────────────────
    public void notifyUserBookingDeclined(Booking booking) {
        Notification n = new Notification();
        n.setUser(booking.getUser());
        n.setBooking(booking);
        n.setType(Notification.NotificationType.BOOKING_DECLINED);
        n.setMessage("❌ <b>" + booking.getWorker().getName() + "</b> has declined your booking request"
                + " for <b>" + booking.getBookingDate() + "</b>."
                + " Please try booking another available worker.");
        notificationRepository.save(n);
    }

    // ─────────────────────────────────────────
    // Called when worker marks booking COMPLETED
    // ─────────────────────────────────────────
    public void notifyUserBookingCompleted(Booking booking) {
        Notification n = new Notification();
        n.setUser(booking.getUser());
        n.setBooking(booking);
        n.setType(Notification.NotificationType.BOOKING_COMPLETED);
        n.setMessage("🎉 Your booking with <b>" + booking.getWorker().getName() + "</b> has been marked as completed!"
                + " Please rate your experience.");
        notificationRepository.save(n);
    }

    // ─────────────────────────────────────────
    // Mark all user notifications as read
    // ─────────────────────────────────────────
    public void markAllReadForUser(User user) {
        notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .forEach(n -> { n.setRead(true); notificationRepository.save(n); });
    }

    // ─────────────────────────────────────────
    // Mark all worker notifications as read
    // ─────────────────────────────────────────
    public void markAllReadForWorker(Worker worker) {
        notificationRepository.findByWorkerOrderByCreatedAtDesc(worker)
                .forEach(n -> { n.setRead(true); notificationRepository.save(n); });
    }

    // ─────────────────────────────────────────
    // Generate 6-digit OTP
    // ─────────────────────────────────────────
    public String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}