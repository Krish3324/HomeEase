package com.homeease.homeease.repository;

import com.homeease.homeease.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findAllByOrderByCreatedAtDesc();
    List<Report> findBySenderEmailOrderByCreatedAtDesc(String email);
    List<Report> findByTargetEmailOrderByCreatedAtDesc(String email);
    List<Report> findByBooking(com.homeease.homeease.model.Booking booking);
    List<Report> findBySenderEmail(String email);
    List<Report> findByTargetEmail(String email);
}
