package com.homeease.homeease.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.homeease.homeease.model.Rating;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.model.Booking;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    
    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.worker = :worker")
    Double getAverageRatingForWorker(@Param("worker") Worker worker);
    
    Optional<Rating> findByBooking(Booking booking);
    
    java.util.List<Rating> findByWorkerOrderByCreatedAtDesc(Worker worker);
}
