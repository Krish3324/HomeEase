package com.homeease.homeease.repository;

import com.homeease.homeease.model.ServiceRequest;
import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    List<ServiceRequest> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT sr FROM ServiceRequest sr JOIN sr.notifiedWorkers w WHERE w = :worker ORDER BY sr.createdAt DESC")
    List<ServiceRequest> findByNotifiedWorker(@Param("worker") Worker worker);
}
