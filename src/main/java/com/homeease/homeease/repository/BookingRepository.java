package com.homeease.homeease.repository;

import com.homeease.homeease.model.Booking;
import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByOrderByCreatedAtDesc();

    List<Booking> findByStatusOrderByCreatedAtDesc(Booking.BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE " +
            "(LOWER(b.user.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(b.worker.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(b.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Booking> searchBookings(@Param("search") String search);

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND " +
            "(LOWER(b.user.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(b.worker.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(b.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Booking> searchBookingsByStatus(@Param("status") Booking.BookingStatus status, @Param("search") String search);

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    List<Booking> findByWorkerOrderByCreatedAtDesc(Worker worker);

    long countByUser(User user);

    long countByUserAndStatus(User user, Booking.BookingStatus status);

    long countByWorker(Worker worker);

    long countByWorkerAndStatus(Worker worker, Booking.BookingStatus status);

    boolean existsByWorkerAndStatusIn(Worker worker, List<Booking.BookingStatus> statuses);

    java.util.Optional<Booking> findFirstByUserAndWorkerAndStatusOrderByCreatedAtDesc(User user, Worker worker, Booking.BookingStatus status);
}