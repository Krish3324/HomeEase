package com.homeease.homeease.repository;

import com.homeease.homeease.model.Booking;
import com.homeease.homeease.model.Message;
import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // Get all messages for a booking, ordered by date
    List<Message> findByBookingOrderBySentAtAsc(Booking booking);

    // Get unread messages for a booking
    List<Message> findByBookingAndIsReadFalse(Booking booking);

    // Count unread messages for a booking
    long countByBookingAndIsReadFalse(Booking booking);

    // Count unread messages for all bookings belonging to a user
    long countByBooking_UserAndIsReadFalse(User user);

    // Count unread messages for all bookings belonging to a worker
    long countByBooking_WorkerAndIsReadFalse(Worker worker);

    // Count all unread messages (admin overview)
    long countByIsReadFalse();

    List<Message> findBySender(com.homeease.homeease.model.User user);
}
