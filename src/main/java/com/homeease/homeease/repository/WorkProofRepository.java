package com.homeease.homeease.repository;

import com.homeease.homeease.model.Booking;
import com.homeease.homeease.model.WorkProof;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkProofRepository extends JpaRepository<WorkProof, Long> {
    List<WorkProof> findByBooking(Booking booking);
    List<WorkProof> findByBookingId(Long bookingId);
}
