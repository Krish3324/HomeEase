package com.homeease.homeease.repository;

import com.homeease.homeease.model.Booking;
import com.homeease.homeease.model.Notification;
import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByWorkerOrderByCreatedAtDesc(Worker worker);

    long countByUserAndReadFalse(User user);

    long countByWorkerAndReadFalse(Worker worker);

    // ✅ NEW: Find the most recent notification for a booking and type (robust for OTP lookup)
    Optional<Notification> findFirstByBookingAndTypeOrderByCreatedAtDesc(Booking booking, Notification.NotificationType type);

    List<Notification> findByBooking(Booking booking);
}