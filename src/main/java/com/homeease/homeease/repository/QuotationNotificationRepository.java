package com.homeease.homeease.repository;

import com.homeease.homeease.model.QuotationNotification;
import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationNotificationRepository extends JpaRepository<QuotationNotification, Long> {

    // Get all notifications for a worker (quotation requests received)
    List<QuotationNotification> findByWorkerOrderByCreatedAtDesc(Worker worker);

    // Get all unread notifications for a worker
    List<QuotationNotification> findByWorkerAndReadFalseOrderByCreatedAtDesc(Worker worker);

    // Count unread notifications for a worker
    long countByWorkerAndReadFalse(Worker worker);

    // Get all notifications for a user (responses to their quotation requests)
    List<QuotationNotification> findByUserOrderByCreatedAtDesc(User user);

    // Get all unread notifications for a user
    List<QuotationNotification> findByUserAndReadFalseOrderByCreatedAtDesc(User user);

    // Count unread notifications for a user
    long countByUserAndReadFalse(User user);

    // Find notification by service request and worker
    Optional<QuotationNotification> findByServiceRequestIdAndWorker(Long serviceRequestId, Worker worker);

    // Find notification by service quote
    Optional<QuotationNotification> findByServiceQuoteId(Long serviceQuoteId);

    // Find all notifications for a specific service request
    List<QuotationNotification> findByServiceRequestIdOrderByCreatedAtDesc(Long serviceRequestId);

    // Find notifications by status
    List<QuotationNotification> findByStatusOrderByCreatedAtDesc(QuotationNotification.QuotationStatus status);

    // Find notifications by type
    List<QuotationNotification> findByNotificationTypeOrderByCreatedAtDesc(QuotationNotification.QuotationType type);
}
