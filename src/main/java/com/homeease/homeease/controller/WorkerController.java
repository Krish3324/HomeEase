package com.homeease.homeease.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Map;

import com.homeease.homeease.model.*;
import com.homeease.homeease.repository.*;
import com.homeease.homeease.service.NotificationService;
import com.homeease.homeease.model.WorkProof;
import com.homeease.homeease.repository.WorkProofRepository;

import org.springframework.web.multipart.MultipartFile;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/worker")
public class WorkerController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private WorkProofRepository workProofRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private BankDetailsRepository bankDetailsRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private com.homeease.homeease.repository.ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private com.homeease.homeease.service.QuotationService quotationService;

    @Autowired
    private com.homeease.homeease.repository.ServiceQuoteRepository serviceQuoteRepository;

    // ================= DASHBOARD =================
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return "redirect:/login";

        model.addAttribute("worker", worker);
        model.addAttribute("bookings", bookingRepository.findByWorkerOrderByCreatedAtDesc(worker));
        model.addAttribute("speakerGuideEnabled", worker.isSpeakerGuideEnabled());

        // Stats
        model.addAttribute("totalCount", bookingRepository.countByWorker(worker));
        model.addAttribute("completedCount",
                bookingRepository.countByWorkerAndStatus(worker, Booking.BookingStatus.COMPLETED));
        model.addAttribute("pendingCount",
                bookingRepository.countByWorkerAndStatus(worker, Booking.BookingStatus.PENDING));
        model.addAttribute("confirmedCount",
                bookingRepository.countByWorkerAndStatus(worker, Booking.BookingStatus.CONFIRMED));
        model.addAttribute("inProgressCount",
                bookingRepository.countByWorkerAndStatus(worker, Booking.BookingStatus.IN_PROGRESS));
        model.addAttribute("cancelledCount",
                bookingRepository.countByWorkerAndStatus(worker, Booking.BookingStatus.CANCELLED));

        return "worker/dashboard";
    }

    // ================= EARNINGS =================
    @GetMapping("/earnings")
    public String earnings(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return "redirect:/login";

        model.addAttribute("worker", worker);
        return "worker/earnings";
    }

    @GetMapping("/api/earnings-data")
    @ResponseBody
    public Map<String, Object> getEarningsData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Principal principal) {
        if (principal == null)
            return Map.of("status", "error", "message", "Unauthorized");
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return Map.of("status", "error", "message", "Unauthorized");

        java.time.LocalDate start = startDate != null && !startDate.isEmpty() ? java.time.LocalDate.parse(startDate)
                : java.time.LocalDate.now().minusDays(30);
        java.time.LocalDate end = endDate != null && !endDate.isEmpty() ? java.time.LocalDate.parse(endDate)
                : java.time.LocalDate.now();

        java.time.LocalDateTime startDateTime = start.atStartOfDay();
        java.time.LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();

        List<Booking> allCompleted = bookingRepository.findByWorkerOrderByCreatedAtDesc(worker)
                .stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED)
                .filter(b -> !b.getCreatedAt().isBefore(startDateTime) && b.getCreatedAt().isBefore(endDateTime))
                .collect(Collectors.toList());

        double totalRevenue = 0;
        List<Map<String, Object>> bookingDetails = new ArrayList<>();

        for (Booking b : allCompleted) {
            double amount = b.getDiscountedAmount() != null ? b.getDiscountedAmount()
                    : (b.getOriginalAmount() != null ? b.getOriginalAmount() : 0.0);
            totalRevenue += amount;

            bookingDetails.add(Map.of(
                    "id", b.getId(),
                    "date", b.getCreatedAt().toLocalDate().toString(),
                    "customerName", b.getUser().getName(),
                    "service",
                    b.getDescription() != null ? b.getDescription()
                            : (b.getWorker().getCategory() != null ? b.getWorker().getCategory() : "Service"),
                    "amount", amount));
        }

        return Map.of(
                "status", "success",
                "totalRevenue", totalRevenue,
                "bookingCount", allCompleted.size(),
                "bookings", bookingDetails);
    }

    // ================= QUOTE REQUESTS =================
    @GetMapping("/quote-requests")
    public String quoteRequests(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return "redirect:/login";

        model.addAttribute("worker", worker);
        model.addAttribute("requests", serviceRequestRepository.findByNotifiedWorker(worker));
        return "worker/quote_requests";
    }

    // ================= MY QUOTES =================
    @GetMapping("/my-quotes")
    public String myQuotes(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return "redirect:/login";

        model.addAttribute("worker", worker);
        List<ServiceQuote> quotes = serviceQuoteRepository.findByWorkerOrderByCreatedAtDesc(worker);
        model.addAttribute("quotes", quotes);

        long pendingCount = quotes.stream().filter(q -> q.getStatus() == ServiceQuote.QuoteStatus.PENDING).count();
        long acceptedCount = quotes.stream().filter(q -> q.getStatus() == ServiceQuote.QuoteStatus.ACCEPTED).count();
        long rejectedCount = quotes.stream().filter(q -> q.getStatus() == ServiceQuote.QuoteStatus.REJECTED).count();

        model.addAttribute("totalCount", quotes.size());
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("acceptedCount", acceptedCount);
        model.addAttribute("rejectedCount", rejectedCount);

        return "worker/my_quotes";
    }

    // ================= QUOTATION NOTIFICATIONS =================
    @GetMapping("/quotation-notifications")
    public String quotationNotifications(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return "redirect:/login";

        model.addAttribute("worker", worker);
        return "worker/quotation_notifications";
    }

    @PostMapping("/accept-request")
    @ResponseBody
    public Map<String, Object> acceptServiceRequest(@RequestParam Long requestId, Principal principal) {
        if (principal == null)
            return Map.of("status", "error", "message", "Unauthorized");
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return Map.of("status", "error", "message", "Unauthorized");

        // CHECK ACTIVE BOOKINGS
        boolean hasActiveBooking = bookingRepository.existsByWorkerAndStatusIn(worker,
                List.of(Booking.BookingStatus.PENDING, Booking.BookingStatus.CONFIRMED, Booking.BookingStatus.IN_PROGRESS));
        if (hasActiveBooking) {
            return Map.of("status", "error", "message", "You cannot accept this request because you have an active ongoing booking.");
        }

        ServiceRequest req = serviceRequestRepository.findById(requestId).orElse(null);
        if (req == null)
            return Map.of("status", "error", "message", "Request not found");

        if (req.getStatus() == ServiceRequest.RequestStatus.CLOSED) {
            return Map.of("status", "error", "message", "This request has already been accepted by another worker.");
        }

        // Create booking
        Booking booking = new Booking();
        booking.setUser(req.getUser());
        booking.setWorker(worker);
        booking.setBookingDate(java.time.LocalDate.now().plusDays(1)); // Default to tomorrow
        booking.setBookingTime(java.time.LocalTime.of(10, 0)); // Default time
        booking.setDescription(req.getProblemDescription());
        booking.setPincode(req.getPincode());
        booking.setStatus(Booking.BookingStatus.CONFIRMED); // Auto-confirm since worker accepted

        Double amount = req.getBudget() != null ? req.getBudget() : (worker.getMinCharge() != null ? worker.getMinCharge() : 0.0);
        booking.setOriginalAmount(amount);
        booking.setDiscountedAmount(amount);

        bookingRepository.save(booking);

        // CREATE A SERVICE QUOTE to track the acceptance history for the user's quotation responses page
        ServiceQuote quote = new ServiceQuote();
        quote.setServiceRequest(req);
        quote.setWorker(worker);
        quote.setAmount(amount);
        quote.setMessage("Accepted directly via broadcast");
        quote.setStatus(ServiceQuote.QuoteStatus.ACCEPTED);
        quote.setCreatedAt(java.time.LocalDateTime.now());
        serviceQuoteRepository.save(quote);

        // Mark request as CLOSED
        req.setStatus(ServiceRequest.RequestStatus.CLOSED);
        serviceRequestRepository.save(req);

        // Notify user
        try {
            notificationService.notifyUserBookingAccepted(booking);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Map.of("status", "success", "message", "Request accepted successfully!");
    }

    @PostMapping("/decline-request")
    @ResponseBody
    public Map<String, Object> declineServiceRequest(@RequestParam Long requestId, Principal principal) {
        if (principal == null)
            return Map.of("status", "error", "message", "Unauthorized");
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return Map.of("status", "error", "message", "Unauthorized");

        ServiceRequest req = serviceRequestRepository.findById(requestId).orElse(null);
        if (req == null)
            return Map.of("status", "error", "message", "Request not found");

        // Remove worker from notified list so it disappears
        req.getNotifiedWorkers().remove(worker);
        serviceRequestRepository.save(req);

        return Map.of("status", "success", "message", "Request declined.");
    }

    // ================= CHAT =================
    @GetMapping("/chat")
    public String chat(@RequestParam Long bookingId, Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return "redirect:/login";

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || !booking.getWorker().getId().equals(worker.getId())) {
            return "redirect:/worker/dashboard";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("worker", worker);
        return "worker/chat";
    }

    // ================= SUBMIT REPORT =================
    @PostMapping("/submit-report")
    public String submitReport(
            @RequestParam String targetEmail,
            @RequestParam String targetRole,
            @RequestParam(required = false) Long bookingId,
            @RequestParam String reason,
            @RequestParam String description,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null)
            return "redirect:/login";

        Report report = new Report();
        report.setSenderEmail(principal.getName());
        report.setSenderRole(Role.WORKER);
        report.setTargetEmail(targetEmail);
        report.setTargetRole(Role.valueOf(targetRole));

        if (bookingId != null) {
            bookingRepository.findById(bookingId).ifPresent(report::setBooking);
        }

        report.setReason(reason);
        report.setDescription(description);
        reportRepository.save(report);

        redirectAttributes.addFlashAttribute("successMsg", "Your report has been submitted.");
        return "redirect:/worker/dashboard";
    }

    // ================= BOOKING ACTIONS =================
    @PostMapping("/bookings/accept")
    @ResponseBody
    public Map<String, Object> acceptBooking(@RequestParam Long bookingId, Principal principal) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null)
            return Map.of("status", "error", "message", "Not found");

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        String otp = notificationService.notifyUserBookingAccepted(booking);
        return Map.of("status", "success", "otp", otp);
    }

    @PostMapping("/bookings/decline")
    @ResponseBody
    public Map<String, String> declineBooking(@RequestParam Long bookingId, Principal principal) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null)
            return Map.of("status", "error", "message", "Not found");

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        return Map.of("status", "success");
    }

    @PostMapping("/bookings/verify-otp")
    @ResponseBody
    public Map<String, String> verifyOtpAndComplete(@RequestParam Long bookingId, @RequestParam String otp,
            Principal principal) {
        try {
            System.out.println("DEBUG: verifyOtpAndComplete called for bookingId=" + bookingId);
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null)
                return Map.of("status", "error", "message", "Booking not found");

            if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
                return Map.of("status", "success", "message", "Already completed");
            }

            Notification notification = notificationRepository
                    .findFirstByBookingAndTypeOrderByCreatedAtDesc(booking,
                            Notification.NotificationType.BOOKING_ACCEPTED)
                    .orElseGet(() -> notificationRepository
                            .findFirstByBookingAndTypeOrderByCreatedAtDesc(booking,
                                    Notification.NotificationType.BOOKING_PLACED)
                            .orElse(null));

            if (notification == null || notification.getOtp() == null) {
                return Map.of("status", "error", "message", "OTP not found for this booking.");
            }

            if (!otp.equals(notification.getOtp())) {
                return Map.of("status", "error", "message", "Invalid OTP. Please enter the correct OTP.");
            }

            booking.setStatus(Booking.BookingStatus.COMPLETED);
            bookingRepository.save(booking);
            notificationService.notifyUserBookingCompleted(booking);
            return Map.of("status", "success", "message", "Booking completed successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("status", "error", "message", "Error: " + e.getMessage());
        }
    }

    @PostMapping("/bookings/start")
    @ResponseBody
    public Map<String, String> startWork(@RequestParam Long bookingId, Principal principal) {
        try {
            System.out.println("DEBUG: startWork called for bookingId=" + bookingId);
            if (principal == null)
                return Map.of("status", "error", "message", "Unauthorized");

            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                System.out.println("DEBUG: Booking not found for ID=" + bookingId);
                return Map.of("status", "error", "message", "Booking not found");
            }

            // Verify ownership
            if (booking.getWorker() == null) {
                System.out.println("DEBUG: Worker is null for booking ID=" + bookingId);
                return Map.of("status", "error", "message", "Booking has no worker assigned");
            }

            System.out.println(
                    "DEBUG: Worker email=" + booking.getWorker().getEmail() + ", Principal=" + principal.getName());
            if (!booking.getWorker().getEmail().equalsIgnoreCase(principal.getName())) {
                return Map.of("status", "error", "message", "Unauthorized: You do not own this booking");
            }

            if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
                System.out.println("DEBUG: Booking status is " + booking.getStatus() + ", expected CONFIRMED");
                return Map.of("status", "error", "message",
                        "Only confirmed bookings can be started. Current status: " + booking.getStatus());
            }

            booking.setStatus(Booking.BookingStatus.IN_PROGRESS);
            bookingRepository.save(booking);
            System.out.println("DEBUG: Booking status updated to IN_PROGRESS");
            return Map.of("status", "success");
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in startWork: " + e.getMessage());
            e.printStackTrace();
            return Map.of("status", "error", "message", "Server Error: " + e.getMessage());
        }
    }

    @PostMapping("/bookings/upload-proof")
    @ResponseBody
    public Map<String, String> uploadProof(
            @RequestParam Long bookingId,
            @RequestParam("images") MultipartFile[] images,
            Principal principal) {
        try {
            if (principal == null)
                return Map.of("status", "error", "message", "Unauthorized");

            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null)
                return Map.of("status", "error", "message", "Booking not found");

            if (booking.getWorker() == null || !booking.getWorker().getEmail().equals(principal.getName())) {
                return Map.of("status", "error", "message", "Unauthorized");
            }

            if (booking.getStatus() != Booking.BookingStatus.IN_PROGRESS
                    && booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
                return Map.of("status", "error", "message",
                        "Cannot upload proofs for this status: " + booking.getStatus());
            }

            String uploadDir = "uploads/work-proofs/booking_" + bookingId;
            Path uploadPath = Paths.get(System.getProperty("user.dir"), uploadDir);
            Files.createDirectories(uploadPath);

            for (MultipartFile file : images) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename() != null
                            ? Paths.get(file.getOriginalFilename()).getFileName().toString()
                            : "file";
                    String fileName = System.currentTimeMillis() + "_" + originalFilename;
                    Path targetPath = uploadPath.resolve(fileName);
                    file.transferTo(targetPath.toFile());

                    WorkProof proof = new WorkProof();
                    proof.setBooking(booking);
                    proof.setImagePath("/" + uploadDir + "/" + fileName);
                    workProofRepository.save(proof);
                }
            }
            return Map.of("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("status", "error", "message", "Server Error: " + e.getMessage());
        }
    }

    @GetMapping("/bookings/proofs")
    @ResponseBody
    public List<Map<String, String>> getProofs(@RequestParam Long bookingId) {
        return workProofRepository.findByBookingId(bookingId).stream()
                .map(p -> Map.of("url", p.getImagePath(), "id", p.getId().toString()))
                .collect(Collectors.toList());
    }

    // ================= HELPER PAGES =================
    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return "redirect:/login";

        model.addAttribute("worker", worker);
        return "worker/profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(
            @RequestParam String name,
            @RequestParam String mobile,
            @RequestParam String city,
            @RequestParam String state,
            @RequestParam String pincode,
            @RequestParam String category,
            @RequestParam(required = false) Integer experience,
            @RequestParam(required = false) Double minCharge,
            @RequestParam(required = false) Double maxCharge,
            @RequestParam(required = false) String bio,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null)
            return "redirect:/login";

        try {
            Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
            if (worker == null)
                return "redirect:/login";

            // Update worker profile fields
            worker.setName(name);
            worker.setMobile(mobile);
            worker.setCity(city);
            worker.setState(state);
            worker.setPincode(pincode);
            worker.setCategory(category);
            if (experience != null)
                worker.setExperience(experience);
            if (minCharge != null)
                worker.setMinCharge(minCharge);
            if (maxCharge != null)
                worker.setMaxCharge(maxCharge);
            if (bio != null && !bio.isBlank())
                worker.setBio(bio);

            workerRepository.save(worker);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error updating profile. Please try again.");
        }

        return "redirect:/worker/profile";
    }

    @GetMapping("/jobs")
    public String jobs() {
        return "worker/jobs";
    }

    // ================= ONLINE/OFFLINE =================
    @PostMapping("/go-online")
    @ResponseBody
    public Map<String, String> goOnline(Principal principal) {
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return Map.of("status", "error");
        worker.setOnline(true);
        workerRepository.save(worker);
        return Map.of("status", "online");
    }

    @PostMapping("/go-offline")
    @ResponseBody
    public Map<String, String> goOffline(Principal principal) {
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return Map.of("status", "error");
        worker.setOnline(false);
        workerRepository.save(worker);
        return Map.of("status", "offline");
    }

    // ================= LOCATION TRACKING =================
    @PostMapping("/update-location")
    @ResponseBody
    public Map<String, String> updateLocation(@RequestParam Double lat, @RequestParam Double lng, Principal principal) {
        if (principal == null)
            return Map.of("status", "error");
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker != null) {
            worker.setLatitude(lat);
            worker.setLongitude(lng);
            workerRepository.save(worker);
            return Map.of("status", "success");
        }
        return Map.of("status", "error");
    }

    // ================= BANK DETAILS MANAGEMENT =================
    @GetMapping("/bank-details")
    public String showBankDetailsPage(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return "redirect:/login";

        // Check if worker already submitted bank details
        BankDetails existing = bankDetailsRepository.findByWorker(worker).orElse(null);
        if (existing != null) {
            model.addAttribute("bankDetails", existing);
            model.addAttribute("hasSubmitted", true);
            model.addAttribute("verificationStatus", existing.getVerificationStatus());
            return "worker/bank-details";
        }

        model.addAttribute("worker", worker);
        model.addAttribute("hasSubmitted", false);
        return "worker/bank-details";
    }

    @PostMapping("/submit-bank-details")
    public String submitBankDetails(
            @ModelAttribute BankDetails bankDetails,
            @RequestParam(value = "bankStatement", required = false) MultipartFile bankStatement,
            @RequestParam(value = "passBook", required = false) MultipartFile passBook,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null)
            return "redirect:/login";
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return "redirect:/login";

        try {
            // Validation
            if (!bankDetails.getAccountNumber().equals(bankDetails.getAccountNumberConfirm())) {
                redirectAttributes.addFlashAttribute("error", "Account numbers don't match!");
                return "redirect:/worker/bank-details";
            }

            // Validate PAN and Aadhar format (basic)
            if (bankDetails.getPanNumber().length() != 10) {
                redirectAttributes.addFlashAttribute("error", "PAN number must be 10 characters long.");
                return "redirect:/worker/bank-details";
            }
            if (bankDetails.getAadharNumber().length() != 12) {
                redirectAttributes.addFlashAttribute("error", "Aadhar number must be 12 digits long.");
                return "redirect:/worker/bank-details";
            }

            // Validate IFSC code
            if (bankDetails.getIfscCode().length() != 11) {
                redirectAttributes.addFlashAttribute("error", "IFSC code must be 11 characters long.");
                return "redirect:/worker/bank-details";
            }

            // Check if already submitted
            BankDetails existing = bankDetailsRepository.findByWorker(worker).orElse(null);
            if (existing != null && existing.getVerificationStatus() == BankDetails.VerificationStatus.PENDING) {
                redirectAttributes.addFlashAttribute("error",
                        "Your bank details are already under verification. Please wait for admin review.");
                return "redirect:/worker/bank-details";
            }

            // Set worker
            bankDetails.setWorker(worker);
            bankDetails.setSubmittedAt(java.time.LocalDateTime.now());

            // Handle file uploads
            if (bankStatement != null && !bankStatement.isEmpty()) {
                if (bankStatement.getSize() > 5 * 1024 * 1024) {
                    redirectAttributes.addFlashAttribute("error", "Bank statement file must be less than 5MB.");
                    return "redirect:/worker/bank-details";
                }
                String fileName = "bank_statement_" + worker.getId() + "_" + System.currentTimeMillis() + ".pdf";
                String uploadDir = "uploads/bank-documents/";
                java.nio.file.Files.createDirectories(java.nio.file.Paths.get(uploadDir));
                bankStatement.transferTo(new java.io.File(uploadDir + fileName));
                bankDetails.setBankStatementPath("/uploads/bank-documents/" + fileName);
            }

            if (passBook != null && !passBook.isEmpty()) {
                if (passBook.getSize() > 5 * 1024 * 1024) {
                    redirectAttributes.addFlashAttribute("error", "Passbook file must be less than 5MB.");
                    return "redirect:/worker/bank-details";
                }
                String fileName = "passbook_" + worker.getId() + "_" + System.currentTimeMillis() + ".pdf";
                String uploadDir = "uploads/bank-documents/";
                java.nio.file.Files.createDirectories(java.nio.file.Paths.get(uploadDir));
                passBook.transferTo(new java.io.File(uploadDir + fileName));
                bankDetails.setPassBookPath("/uploads/bank-documents/" + fileName);
            }

            bankDetailsRepository.save(bankDetails);
            worker.setBankDetailsSubmitted(true);
            workerRepository.save(worker);

            redirectAttributes.addFlashAttribute("success",
                    "Bank details submitted successfully! Your details are under review by admin.");
            return "redirect:/worker/bank-details";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error submitting bank details. Please try again.");
            return "redirect:/worker/bank-details";
        }
    }

    @GetMapping("/check-bank-verification")
    @ResponseBody
    public Map<String, Object> checkBankVerification(Principal principal) {
        if (principal == null)
            return Map.of("status", "error");

        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return Map.of("status", "error");

        BankDetails bankDetails = bankDetailsRepository.findByWorker(worker).orElse(null);
        if (bankDetails == null) {
            return Map.of("status", "not_submitted", "verified", false);
        }

        return Map.of(
                "status", "submitted",
                "verificationStatus", bankDetails.getVerificationStatus().toString(),
                "verified", bankDetails.getVerificationStatus() == BankDetails.VerificationStatus.VERIFIED,
                "rejectionReason", bankDetails.getRejectionReason() != null ? bankDetails.getRejectionReason() : "");
    }

    // ================= SETTINGS =================
    @GetMapping("/settings")
    public String settings(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return "redirect:/login";

        model.addAttribute("worker", worker);
        model.addAttribute("speakerGuideEnabled", worker.isSpeakerGuideEnabled());
        return "worker/settings";
    }

    @PostMapping("/toggle-speaker-guide")
    @ResponseBody
    public Map<String, Object> toggleSpeakerGuide(Principal principal) {
        if (principal == null)
            return Map.of("status", "error", "message", "Unauthorized");

        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return Map.of("status", "error", "message", "Worker not found");

        boolean newStatus = !worker.isSpeakerGuideEnabled();
        worker.setSpeakerGuideEnabled(newStatus);
        workerRepository.save(worker);

        return Map.of(
                "status", "success",
                "message", "Speaker guide " + (newStatus ? "enabled" : "disabled"),
                "enabled", newStatus
        );
    }

    @GetMapping("/get-speaker-guide-status")
    @ResponseBody
    public Map<String, Object> getSpeakerGuideStatus(Principal principal) {
        if (principal == null)
            return Map.of("status", "error", "message", "Unauthorized");

        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null)
            return Map.of("status", "error", "message", "Worker not found");

        return Map.of(
                "status", "success",
                "enabled", worker.isSpeakerGuideEnabled()
        );
    }

}
