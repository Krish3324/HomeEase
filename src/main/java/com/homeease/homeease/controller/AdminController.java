package com.homeease.homeease.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.homeease.homeease.model.Role;
import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.model.Booking;
import com.homeease.homeease.model.Report;
import com.homeease.homeease.model.BankDetails;
import com.homeease.homeease.model.ServiceCategory;
import com.homeease.homeease.repository.BankDetailsRepository;
import com.homeease.homeease.repository.BookingRepository;
import com.homeease.homeease.repository.ReportRepository;
import com.homeease.homeease.repository.ServiceCategoryRepository;
import com.homeease.homeease.repository.UserRepository;
import com.homeease.homeease.repository.WorkerRepository;
import com.homeease.homeease.service.AdminService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private BankDetailsRepository bankDetailsRepository;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private com.homeease.homeease.repository.WorkProofRepository workProofRepository;

    @Autowired
    private com.homeease.homeease.service.EmailService emailService;

    // ===============================
    // DASHBOARD - Overview & Stats
    // ===============================
    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        long totalUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .count();

        long activeWorkers = workerRepository.countByApprovedTrue();
        long pendingWorkers = workerRepository.countByApprovedFalse();
        long totalWorkers = workerRepository.count();
        long totalBookings = bookingRepository.count();

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalWorkers", totalWorkers);
        model.addAttribute("activeWorkers", activeWorkers);
        model.addAttribute("pendingWorkers", pendingWorkers);
        model.addAttribute("totalBookings", totalBookings);

        // Chart data
        model.addAttribute("activeWorkersChart", activeWorkers);
        model.addAttribute("pendingWorkersChart", pendingWorkers);

        // Category & City analytics
        List<Object[]> categoryData = workerRepository.countWorkersByCategory();
        List<Object[]> cityData = workerRepository.countWorkersByCity();

        model.addAttribute("categoryLabels", categoryData.stream().map(o -> o[0]).toList());
        model.addAttribute("categoryCounts", categoryData.stream().map(o -> o[1]).toList());
        model.addAttribute("cityLabels", cityData.stream().map(o -> o[0]).toList());
        model.addAttribute("cityCounts", cityData.stream().map(o -> o[1]).toList());

        return "admin/dashboard";
    }

    // ===============================
    // WORKER MANAGEMENT
    // ===============================
    @GetMapping("/workers")
    public String allWorkers(@RequestParam(required = false) String search, Model model) {
        if (search != null && !search.isEmpty()) {
            model.addAttribute("allWorkers", workerRepository.searchWorkers(search));
            model.addAttribute("pendingWorkers", workerRepository.searchPendingWorkers(search));
            model.addAttribute("activeWorkers", workerRepository.searchActiveWorkers(search));
            model.addAttribute("search", search);
        } else {
            model.addAttribute("allWorkers", workerRepository.findAll());
            model.addAttribute("pendingWorkers", workerRepository.findByApprovedFalse());
            model.addAttribute("activeWorkers", workerRepository.findByApprovedTrue());
        }
        return "admin/workers";
    }

    @GetMapping("/workers/pending")
    public String pendingWorkers(@RequestParam(required = false) String search, Model model) {
        if (search != null && !search.isEmpty()) {
            model.addAttribute("pendingWorkers", workerRepository.searchPendingWorkers(search));
            model.addAttribute("search", search);
        } else {
            model.addAttribute("pendingWorkers", workerRepository.findByApprovedFalse());
        }
        return "admin/workers";
    }

    @GetMapping("/active")
    public String activeWorkers(@RequestParam(required = false) String search, Model model) {
        List<Worker> activeWorkers;
        if (search != null && !search.isEmpty()) {
            activeWorkers = workerRepository.searchActiveWorkers(search);
            model.addAttribute("search", search);
        } else {
            activeWorkers = workerRepository.findByApprovedTrue();
        }

        if (activeWorkers == null) {
            activeWorkers = new ArrayList<>();
        }

        model.addAttribute("activeWorkers", activeWorkers);
        return "admin/active-workers";
    }

    @GetMapping("/services")
    public String services(Model model) {
        var serviceCategories = serviceCategoryRepository.findAll();
        var workerCounts = new java.util.HashMap<String, Long>();

        for (ServiceCategory category : serviceCategories) {
            workerCounts.put(category.getName(), workerRepository.countAvailableByCategory(category.getName()));
        }

        model.addAttribute("serviceCategories", serviceCategories);
        model.addAttribute("workerCounts", workerCounts);
        return "admin/services";
    }

    @PostMapping("/services/add")
    public String addServiceCategory(@RequestParam String name,
                                     @RequestParam(required = false) String description,
                                     RedirectAttributes redirectAttributes) {
        if (name == null || name.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Service name cannot be empty.");
            return "redirect:/admin/services";
        }

        if (serviceCategoryRepository.findByNameIgnoreCase(name).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMsg", "This service already exists.");
            return "redirect:/admin/services";
        }

        ServiceCategory category = new ServiceCategory();
        category.setName(name.trim());
        category.setDescription(description != null ? description.trim() : "");
        serviceCategoryRepository.save(category);
        redirectAttributes.addFlashAttribute("successMsg", "Service category added successfully.");
        return "redirect:/admin/services";
    }

    @PostMapping("/services/delete")
    public String deleteServiceCategory(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        serviceCategoryRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMsg", "Service category deleted successfully.");
        return "redirect:/admin/services";
    }

    @PostMapping("/workers/approve")
    public String approveWorker(@RequestParam Long id, RedirectAttributes redirectAttributes) {

        Worker worker = workerRepository.findById(id).orElse(null);

        if (worker != null) {
            worker.setApproved(true);
            worker.setEnabled(true);
            workerRepository.save(worker);

            // Send notification email
            emailService.sendWorkerApprovedEmail(worker.getEmail(), worker.getName());

            redirectAttributes.addFlashAttribute("successMsg",
                    "Worker " + worker.getName() + " approved successfully and notified via email!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Worker not found!");
        }

        return "redirect:/admin/workers";
    }

    @PostMapping("/workers/reject")
    public String rejectWorker(@RequestParam Long id, RedirectAttributes redirectAttributes) {

        Worker worker = workerRepository.findById(id).orElse(null);

        if (worker != null) {
            worker.setApproved(false);
            worker.setEnabled(false);
            workerRepository.save(worker);

            // Send notification email
            emailService.sendWorkerDeclinedEmail(worker.getEmail(), worker.getName());

            redirectAttributes.addFlashAttribute("successMsg",
                    "Worker " + worker.getName() + " declined and notified via email!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Worker not found!");
        }

        return "redirect:/admin/workers";
    }

    @PostMapping("/workers/disable")
    public String disableWorker(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        Worker worker = workerRepository.findById(id).orElse(null);
        if (worker != null) {
            worker.setEnabled(false);
            workerRepository.save(worker);
            redirectAttributes.addFlashAttribute("successMsg", "Worker " + worker.getName() + " disabled!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Worker not found!");
        }
        return "redirect:/admin/active";
    }

    @PostMapping("/workers/enable")
    public String enableWorker(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        Worker worker = workerRepository.findById(id).orElse(null);
        if (worker != null) {
            worker.setEnabled(true);
            workerRepository.save(worker);
            redirectAttributes.addFlashAttribute("successMsg", "Worker " + worker.getName() + " enabled successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Worker not found!");
        }
        return "redirect:/admin/active";
    }

    @PostMapping("/workers/delete")
    public String deleteWorker(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteWorker(id);
            redirectAttributes.addFlashAttribute("successMsg", "Worker and all associated data deleted successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = "Error deleting worker: " + e.getMessage();
            if (e.getCause() != null) {
                errorMsg += " | Cause: " + e.getCause().getMessage();
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }
        return "redirect:/admin/active";
    }

    @GetMapping("/workers/details/{id}")
    public String viewWorkerDetails(@PathVariable Long id, Model model) {

        Worker worker = workerRepository.findById(id).orElse(null);

        model.addAttribute("worker", worker);

        return "admin/worker-details";
    }

    @GetMapping("/workers/details-json/{id}")
    @ResponseBody
    public Worker getWorkerDetailsJson(@PathVariable Long id) {
        return workerRepository.findById(id).orElse(null);
    }

    // ===============================
    // VIEW WORKER ID IMAGE (ADMIN ONLY)
    // ===============================
    @GetMapping("/workers/view-id/{id}")
    public ResponseEntity<Resource> viewWorkerId(@PathVariable Long id) throws IOException {

        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        Path path = Paths.get(worker.getIdImagePath());

        if (!Files.exists(path)) {
            throw new RuntimeException("File not found");
        }

        Resource resource = new UrlResource(path.toUri());

        String contentType = Files.probeContentType(path);

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/work-proofs")
    public String workProofs(@RequestParam(required = false) String search, Model model) {
        List<com.homeease.homeease.model.WorkProof> proofs;
        if (search != null && !search.isEmpty()) {
            proofs = workProofRepository.findAll().stream()
                .filter(p -> p.getBooking().getWorker().getName().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
        } else {
            proofs = workProofRepository.findAll();
        }
        
        model.addAttribute("proofs", proofs);
        model.addAttribute("search", search);
        return "admin/work-proofs";
    }

    @GetMapping("/bookings/proofs/{bookingId}")
    @ResponseBody
    public List<com.homeease.homeease.model.WorkProof> getBookingProofs(@PathVariable Long bookingId) {
        return workProofRepository.findByBookingId(bookingId);
    }

    // ===============================
    // USER MANAGEMENT
    // ===============================
    @GetMapping("/users")
    public String allUsers(@RequestParam(required = false) String search, Model model) {
        if (search != null && !search.isEmpty()) {
            model.addAttribute("allUsers", userRepository.searchUsers(Role.USER, search));
            model.addAttribute("search", search);
        } else {
            model.addAttribute("allUsers", userRepository.findByRole(Role.USER));
        }
        return "admin/users";
    }

    @PostMapping("/users/disable")
    public String disableUser(@RequestParam Long id, RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id).orElse(null);

        if (user != null && user.getRole() != Role.ADMIN) {
            user.setEnabled(false);
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("successMsg",
                    "User " + user.getName() + " disabled!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg",
                    "Cannot disable admin or user not found!");
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/enable")
    public String enableUser(@RequestParam Long id, RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id).orElse(null);

        if (user != null) {
            user.setEnabled(true);
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("successMsg",
                    "User " + user.getName() + " enabled!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "User not found!");
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMsg", "User and all associated data deleted successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = "Error deleting user: " + e.getMessage();
            if (e.getCause() != null) {
                errorMsg += " | Cause: " + e.getCause().getMessage();
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }
        return "redirect:/admin/users";
    }

    // ===============================
    // BOOKING MANAGEMENT
    // ===============================
    @GetMapping("/bookings")
    public String allBookings(@RequestParam(required = false) String search, Model model) {
        if (search != null && !search.isEmpty()) {
            model.addAttribute("bookings", bookingRepository.searchBookings(search));
            model.addAttribute("search", search);
        } else {
            model.addAttribute("bookings", bookingRepository.findAllByOrderByCreatedAtDesc());
        }
        model.addAttribute("viewTitle", "All Bookings");
        return "admin/bookings";
    }

    @GetMapping("/bookings/completed")
    public String completedBookings(@RequestParam(required = false) String search, Model model) {
        if (search != null && !search.isEmpty()) {
            model.addAttribute("bookings", bookingRepository.searchBookingsByStatus(Booking.BookingStatus.COMPLETED, search));
            model.addAttribute("search", search);
        } else {
            model.addAttribute("bookings", bookingRepository.findByStatusOrderByCreatedAtDesc(Booking.BookingStatus.COMPLETED));
        }
        model.addAttribute("viewTitle", "Completed Bookings");
        model.addAttribute("isCompleted", true);
        return "admin/bookings";
    }

    // ===============================
    // SETTINGS
    // ===============================
    @GetMapping("/settings")
    public String settings() {
        return "admin/settings";
    }

    // ===============================
    // REPORT MANAGEMENT
    // ===============================
    @GetMapping("/reports")
    public String allReports(Model model) {
        model.addAttribute("reports", reportRepository.findAllByOrderByCreatedAtDesc());
        return "admin/reports";
    }

    @GetMapping("/chat")
    public String chat(@RequestParam Long bookingId, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User admin = userRepository.findByEmail(principal.getName()).orElse(null);
        if (admin == null || admin.getRole() != Role.ADMIN) {
            return "redirect:/admin/dashboard";
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return "redirect:/admin/reports";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("adminName", admin.getName());
        return "admin/chat";
    }

    @PostMapping("/reports/resolve")
    public String resolveReport(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        Report report = reportRepository.findById(id).orElse(null);
        if (report != null) {
            report.setStatus(Report.ReportStatus.RESOLVED);
            reportRepository.save(report);
            redirectAttributes.addFlashAttribute("successMsg", "Report #" + id + " marked as resolved.");
        }
        return "redirect:/admin/reports";
    }

    // ===============================
    // BANK DETAILS VERIFICATION
    // ===============================
    @GetMapping("/bank-details")
    public String bankDetailsVerification(
            @RequestParam(required = false) String filter,
            Model model) {
        
        List<BankDetails> bankDetailsList = new ArrayList<>();
        
        if ("pending".equals(filter)) {
            bankDetailsList = bankDetailsRepository.findPendingForVerification();
        } else if ("verified".equals(filter)) {
            bankDetailsList = bankDetailsRepository.findVerifiedBankDetails();
        } else if ("rejected".equals(filter)) {
            bankDetailsList = bankDetailsRepository.findRejectedBankDetails();
        } else if ("resubmit".equals(filter)) {
            bankDetailsList = bankDetailsRepository.findNeedsResubmission();
        } else {
            bankDetailsList = bankDetailsRepository.findAll();
        }
        
        model.addAttribute("bankDetailsList", bankDetailsList);
        model.addAttribute("filter", filter != null ? filter : "all");
        model.addAttribute("pendingCount", bankDetailsRepository.findPendingForVerification().size());
        model.addAttribute("verifiedCount", bankDetailsRepository.findVerifiedBankDetails().size());
        
        return "admin/bank-details";
    }

    @GetMapping("/bank-details/{id}")
    public String viewBankDetails(@PathVariable Long id, Model model) {
        BankDetails bankDetails = bankDetailsRepository.findById(id).orElse(null);
        if (bankDetails == null) {
            return "redirect:/admin/bank-details";
        }
        
        model.addAttribute("bankDetails", bankDetails);
        model.addAttribute("worker", bankDetails.getWorker());
        return "admin/bank-details-view";
    }

    @PostMapping("/bank-details/verify")
    public String verifyBankDetails(
            @RequestParam Long id,
            @RequestParam(required = false) String adminNotes,
            RedirectAttributes redirectAttributes) {
        
        BankDetails bankDetails = bankDetailsRepository.findById(id).orElse(null);
        if (bankDetails == null) {
            redirectAttributes.addFlashAttribute("error", "Bank details not found!");
            return "redirect:/admin/bank-details";
        }
        
        try {
            bankDetails.setVerificationStatus(BankDetails.VerificationStatus.VERIFIED);
            bankDetails.setVerifiedAt(java.time.LocalDateTime.now());
            if (adminNotes != null && !adminNotes.isBlank()) {
                bankDetails.setAdminNotes(adminNotes);
            }
            bankDetailsRepository.save(bankDetails);
            
            // Update worker to show bank details verified
            Worker worker = bankDetails.getWorker();
            worker.setBankDetailsVerified(true);
            workerRepository.save(worker);
            
            redirectAttributes.addFlashAttribute("success", 
                "Bank details for " + worker.getName() + " verified successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error verifying bank details!");
        }
        
        return "redirect:/admin/bank-details";
    }

    @PostMapping("/bank-details/reject")
    public String rejectBankDetails(
            @RequestParam Long id,
            @RequestParam String rejectionReason,
            @RequestParam(required = false) String adminNotes,
            RedirectAttributes redirectAttributes) {
        
        BankDetails bankDetails = bankDetailsRepository.findById(id).orElse(null);
        if (bankDetails == null) {
            redirectAttributes.addFlashAttribute("error", "Bank details not found!");
            return "redirect:/admin/bank-details";
        }
        
        try {
            bankDetails.setVerificationStatus(BankDetails.VerificationStatus.REJECTED);
            bankDetails.setRejectionReason(rejectionReason);
            if (adminNotes != null && !adminNotes.isBlank()) {
                bankDetails.setAdminNotes(adminNotes);
            }
            bankDetailsRepository.save(bankDetails);
            
            redirectAttributes.addFlashAttribute("success", 
                "Bank details rejected. Worker will be notified.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error rejecting bank details!");
        }
        
        return "redirect:/admin/bank-details";
    }

    @PostMapping("/bank-details/request-resubmission")
    public String requestResubmission(
            @RequestParam Long id,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {
        
        BankDetails bankDetails = bankDetailsRepository.findById(id).orElse(null);
        if (bankDetails == null) {
            redirectAttributes.addFlashAttribute("error", "Bank details not found!");
            return "redirect:/admin/bank-details";
        }
        
        try {
            bankDetails.setVerificationStatus(BankDetails.VerificationStatus.NEEDS_RESUBMISSION);
            bankDetails.setRejectionReason(reason);
            bankDetailsRepository.save(bankDetails);
            
            redirectAttributes.addFlashAttribute("success", 
                "Resubmission request sent to worker.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error sending resubmission request!");
        }
        
        return "redirect:/admin/bank-details";
    }
}
