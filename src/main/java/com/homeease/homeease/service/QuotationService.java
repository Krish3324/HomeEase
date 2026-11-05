package com.homeease.homeease.service;

import com.homeease.homeease.model.*;
import com.homeease.homeease.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuotationService {

    @Autowired
    private QuotationNotificationRepository quotationNotificationRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private ServiceQuoteRepository serviceQuoteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    /**
     * Send quotation request to workers
     * Called when user creates a new service request
     */
    public void sendQuotationRequests(ServiceRequest request, List<Worker> workers) {
        for (Worker worker : workers) {
            QuotationNotification notification = new QuotationNotification();
            notification.setUser(request.getUser());
            notification.setWorker(worker);
            notification.setServiceRequest(request);
            notification.setNotificationType(QuotationNotification.QuotationType.REQUEST_SENT);
            notification.setStatus(QuotationNotification.QuotationStatus.PENDING);
            notification.setMessage(
                    "New quotation request: " + request.getCategory() + " - " + request.getProblemDescription());
            notification.setCreatedAt(LocalDateTime.now());
            notification.setUpdatedAt(LocalDateTime.now());

            quotationNotificationRepository.save(notification);
        }
    }

    /**
     * Worker submits a quote
     */
    public QuotationNotification submitQuote(Long requestId, Long workerId, Double amount, String message)
            throws Exception {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Service request not found"));

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new Exception("Worker not found"));

        // Create or update service quote
        ServiceQuote quote = serviceQuoteRepository.findByServiceRequestAndWorker(request, worker)
                .orElse(new ServiceQuote());

        quote.setServiceRequest(request);
        quote.setWorker(worker);
        quote.setAmount(amount);
        quote.setMessage(message);
        quote.setStatus(ServiceQuote.QuoteStatus.PENDING);
        quote.setCreatedAt(LocalDateTime.now());

        quote = serviceQuoteRepository.save(quote);

        // Find or create quotation notification
        QuotationNotification notification = quotationNotificationRepository
                .findByServiceRequestIdAndWorker(requestId, worker)
                .orElseGet(() -> {
                    QuotationNotification n = new QuotationNotification();
                    n.setUser(request.getUser());
                    n.setWorker(worker);
                    n.setServiceRequest(request);
                    return n;
                });

        notification.setServiceQuote(quote);
        notification.setNotificationType(QuotationNotification.QuotationType.QUOTE_SUBMITTED);
        notification.setStatus(QuotationNotification.QuotationStatus.PENDING);
        notification.setQuoteAmount(amount);
        notification.setMessage("Quote submitted: ₹" + amount);
        notification.setRead(false);
        notification.setUserNotified(false);
        notification.setUpdatedAt(LocalDateTime.now());

        return quotationNotificationRepository.save(notification);
    }

    /**
     * User accepts a quote
     */
    public Booking acceptQuote(Long quoteId, Long userId) throws Exception {
        ServiceQuote quote = serviceQuoteRepository.findById(quoteId)
                .orElseThrow(() -> new Exception("Quote not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        if (!quote.getServiceRequest().getUser().getId().equals(userId)) {
            throw new Exception("Unauthorized");
        }

        // VALIDATE: worker must not have any active bookings
        boolean workerIsBusy = bookingRepository.existsByWorkerAndStatusIn(
            quote.getWorker(),
            List.of(Booking.BookingStatus.PENDING, Booking.BookingStatus.CONFIRMED, Booking.BookingStatus.IN_PROGRESS)
        );
        if (workerIsBusy) {
            throw new Exception("This worker currently has an active booking and cannot accept new jobs until it is completed.");
        }

        // Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setWorker(quote.getWorker());
        booking.setBookingDate(java.time.LocalDate.now().plusDays(1));
        booking.setOriginalAmount(quote.getAmount());
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        if (quote.getServiceRequest().getLatitude() != null) {
            booking.setLatitude(quote.getServiceRequest().getLatitude());
        }
        if (quote.getServiceRequest().getLongitude() != null) {
            booking.setLongitude(quote.getServiceRequest().getLongitude());
        }

        booking = bookingRepository.save(booking);

        // Update quote status
        quote.setStatus(ServiceQuote.QuoteStatus.ACCEPTED);
        serviceQuoteRepository.save(quote);

        // Update service request status
        ServiceRequest request = quote.getServiceRequest();
        request.setStatus(ServiceRequest.RequestStatus.CLOSED);
        serviceRequestRepository.save(request);

        // Create quotation notification
        QuotationNotification notification = quotationNotificationRepository
                .findByServiceRequestIdAndWorker(request.getId(), quote.getWorker())
                .orElseGet(() -> {
                    QuotationNotification n = new QuotationNotification();
                    n.setUser(user);
                    n.setWorker(quote.getWorker());
                    n.setServiceRequest(request);
                    return n;
                });

        notification.setServiceQuote(quote);
        notification.setBooking(booking);
        notification.setNotificationType(QuotationNotification.QuotationType.QUOTE_ACCEPTED);
        notification.setStatus(QuotationNotification.QuotationStatus.ACCEPTED);
        notification.setMessage("Your quote has been accepted! Booking ID: " + booking.getId());
        notification.setRead(false);
        notification.setUpdatedAt(LocalDateTime.now());

        quotationNotificationRepository.save(notification);

        return booking;
    }

    /**
     * User rejects a quote
     */
    public void rejectQuote(Long quoteId, Long userId) throws Exception {
        ServiceQuote quote = serviceQuoteRepository.findById(quoteId)
                .orElseThrow(() -> new Exception("Quote not found"));

        if (!quote.getServiceRequest().getUser().getId().equals(userId)) {
            throw new Exception("Unauthorized");
        }

        quote.setStatus(ServiceQuote.QuoteStatus.REJECTED);
        serviceQuoteRepository.save(quote);

        // Update notification
        QuotationNotification notification = quotationNotificationRepository
                .findByServiceRequestIdAndWorker(quote.getServiceRequest().getId(), quote.getWorker())
                .orElse(null);

        if (notification != null) {
            notification.setNotificationType(QuotationNotification.QuotationType.REQUEST_SENT);
            notification.setStatus(QuotationNotification.QuotationStatus.REJECTED);
            notification.setMessage("Your quote has been rejected");
            notification.setUpdatedAt(LocalDateTime.now());
            quotationNotificationRepository.save(notification);
        }
    }

    /**
     * User cancels a quotation request
     */
    public void cancelRequest(Long requestId, Long userId) throws Exception {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Service request not found"));

        if (!request.getUser().getId().equals(userId)) {
            throw new Exception("Unauthorized");
        }

        request.setStatus(ServiceRequest.RequestStatus.CLOSED);
        serviceRequestRepository.save(request);

        // Update all worker notifications
        List<QuotationNotification> notifications = quotationNotificationRepository
                .findByServiceRequestIdOrderByCreatedAtDesc(requestId);

        for (QuotationNotification notification : notifications) {
            notification.setStatus(QuotationNotification.QuotationStatus.CANCELLED);
            notification.setNotificationType(QuotationNotification.QuotationType.REQUEST_CANCELLED);
            notification.setMessage("Request cancelled by user");
            notification.setUpdatedAt(LocalDateTime.now());
            quotationNotificationRepository.save(notification);
        }
    }

    /**
     * Get worker's quotation notifications
     */
    public List<Map<String, Object>> getWorkerQuotationNotifications(Long workerId) throws Exception {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new Exception("Worker not found"));

        List<QuotationNotification> notifications = quotationNotificationRepository
                .findByWorkerOrderByCreatedAtDesc(worker);

        return notifications.stream().map(this::notificationToMap).collect(Collectors.toList());
    }

    /**
     * Get user's quotation notifications (responses to their requests)
     */
    public List<Map<String, Object>> getUserQuotationNotifications(Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        List<QuotationNotification> notifications = quotationNotificationRepository
                .findByUserOrderByCreatedAtDesc(user);

        return notifications.stream().map(this::notificationToMap).collect(Collectors.toList());
    }

    /**
     * Get unread notifications count for worker
     */
    public long getUnreadWorkerNotificationsCount(Long workerId) throws Exception {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new Exception("Worker not found"));

        return quotationNotificationRepository.countByWorkerAndReadFalse(worker);
    }

    /**
     * Get unread notifications count for user
     */
    public long getUnreadUserNotificationsCount(Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        return quotationNotificationRepository.countByUserAndReadFalse(user);
    }

    /**
     * Mark notification as read
     */
    public void markAsRead(Long notificationId) throws Exception {
        QuotationNotification notification = quotationNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new Exception("Notification not found"));

        notification.setRead(true);
        notification.setUpdatedAt(LocalDateTime.now());
        quotationNotificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read for user
     */
    public void markAllUserNotificationsAsRead(Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        List<QuotationNotification> notifications = quotationNotificationRepository
                .findByUserAndReadFalseOrderByCreatedAtDesc(user);

        for (QuotationNotification notification : notifications) {
            notification.setRead(true);
            notification.setUpdatedAt(LocalDateTime.now());
            quotationNotificationRepository.save(notification);
        }
    }

    /**
     * Mark all notifications as read for worker
     */
    public void markAllWorkerNotificationsAsRead(Long workerId) throws Exception {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new Exception("Worker not found"));

        List<QuotationNotification> notifications = quotationNotificationRepository
                .findByWorkerAndReadFalseOrderByCreatedAtDesc(worker);

        for (QuotationNotification notification : notifications) {
            notification.setRead(true);
            notification.setUpdatedAt(LocalDateTime.now());
            quotationNotificationRepository.save(notification);
        }
    }

    /**
     * Get quotation history for user (all their quotation requests with responses)
     */
    public List<Map<String, Object>> getUserQuotationHistory(Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        List<ServiceRequest> requests = serviceRequestRepository.findByUserOrderByCreatedAtDesc(user);

        return requests.stream().map(request -> {
            Map<String, Object> map = new HashMap<>();
            map.put("requestId", request.getId());
            map.put("category", request.getCategory());
            map.put("description", request.getProblemDescription());
            map.put("budget", request.getBudget());
            map.put("urgency", request.getUrgency());
            map.put("pincode", request.getPincode());
            map.put("status", request.getStatus().name());
            map.put("createdAt", request.getCreatedAt());

            // Get all quotes for this request
            List<Map<String, Object>> quotes = request.getQuotes().stream().map(quote -> {
                Map<String, Object> quoteMap = new HashMap<>();
                quoteMap.put("quoteId", quote.getId());
                quoteMap.put("workerId", quote.getWorker().getId());
                quoteMap.put("workerName", quote.getWorker().getName());
                quoteMap.put("workerRating", quote.getWorker().getRating());
                quoteMap.put("workerProfilePhoto", quote.getWorker().getProfileImage());
                quoteMap.put("amount", quote.getAmount());
                quoteMap.put("message", quote.getMessage());
                quoteMap.put("status", quote.getStatus().name());
                quoteMap.put("createdAt", quote.getCreatedAt());

                // Get notification for this quote
                QuotationNotification notification = quotationNotificationRepository
                        .findByServiceRequestIdAndWorker(request.getId(), quote.getWorker())
                        .orElse(null);

                if (notification != null) {
                    quoteMap.put("notificationId", notification.getId());
                    quoteMap.put("notificationType", notification.getNotificationType().name());
                    quoteMap.put("notificationStatus", notification.getStatus().name());
                    quoteMap.put("isRead", notification.isRead());
                }

                return quoteMap;
            }).collect(Collectors.toList());

            map.put("quotes", quotes);
            map.put("quoteCount", quotes.size());

            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Convert notification to map for response
     */
    private Map<String, Object> notificationToMap(QuotationNotification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("notificationId", notification.getId());
        map.put("type", notification.getNotificationType().name());
        map.put("status", notification.getStatus().name());
        map.put("message", notification.getMessage());
        map.put("isRead", notification.isRead());
        map.put("createdAt", notification.getCreatedAt());
        map.put("updatedAt", notification.getUpdatedAt());

        if (notification.getServiceRequest() != null) {
            map.put("requestId", notification.getServiceRequest().getId());
            map.put("category", notification.getServiceRequest().getCategory());
            map.put("description", notification.getServiceRequest().getProblemDescription());
            map.put("budget", notification.getServiceRequest().getBudget());
        }

        if (notification.getServiceQuote() != null) {
            map.put("quoteId", notification.getServiceQuote().getId());
            map.put("quoteAmount", notification.getServiceQuote().getAmount());
            map.put("quoteMessage", notification.getServiceQuote().getMessage());
        }

        if (notification.getUser() != null) {
            map.put("userId", notification.getUser().getId());
            map.put("userName", notification.getUser().getName());
            map.put("userPhone", notification.getUser().getMobile());
        }

        if (notification.getWorker() != null) {
            map.put("workerId", notification.getWorker().getId());
            map.put("workerName", notification.getWorker().getName());
            map.put("workerPhone", notification.getWorker().getMobile());
        }

        if (notification.getBooking() != null) {
            map.put("bookingId", notification.getBooking().getId());
        }

        return map;
    }
}
