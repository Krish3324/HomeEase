package com.homeease.homeease.repository;

import com.homeease.homeease.model.Booking;
import com.homeease.homeease.model.Payment;
import com.homeease.homeease.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBooking(Booking booking);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    List<Payment> findByUserOrderByCreatedAtDesc(User user);
}