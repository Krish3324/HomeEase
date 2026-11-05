package com.homeease.homeease.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.homeease.homeease.model.Booking;
import com.homeease.homeease.model.Role;
import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.model.Payment;
import com.homeease.homeease.model.Rating;
import com.homeease.homeease.model.Report;
import com.homeease.homeease.model.ServiceRequest;
import com.homeease.homeease.model.ServiceQuote;
import com.homeease.homeease.repository.BookingRepository;
import com.homeease.homeease.repository.PaymentRepository;
import com.homeease.homeease.repository.UserRepository;
import com.homeease.homeease.repository.WorkerRepository;
import com.homeease.homeease.repository.ServiceCategoryRepository;
import com.homeease.homeease.repository.RatingRepository;
import com.homeease.homeease.repository.ReportRepository;
import com.homeease.homeease.repository.ServiceRequestRepository;
import com.homeease.homeease.repository.ServiceQuoteRepository;
import com.homeease.homeease.service.NotificationService;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private com.homeease.homeease.service.PaymentService paymentService;

    @Autowired
    private com.homeease.homeease.repository.NotificationRepository notificationRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private ServiceQuoteRepository serviceQuoteRepository;

    @Autowired
    private com.homeease.homeease.service.QuotationService quotationService;

    @ModelAttribute("unreadNotificationsCount")
    public long getUnreadNotificationsCount(Principal principal) {
        if (principal == null)
            return 0;
        return userRepository.findByEmail(principal.getName())
                .map(notificationRepository::countByUserAndReadFalse)
                .orElse(0L);
    }

    // ================= REQUEST QUOTES =================
    @PostMapping("/request-quotes")
    @ResponseBody
    public Map<String, Object> requestQuotes(
            @RequestParam String category,
            @RequestParam String problemDescription,
            @RequestParam(required = false) Double budget,
            @RequestParam String urgency,
            @RequestParam String pincode,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            Principal principal) {

        if (principal == null)
            return Map.of("status", "error", "message", "Please login first.");
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null)
            return Map.of("status", "error", "message", "User not found.");

        // Find up to 10 nearby workers in that category+pincode
        List<Worker> nearby = workerRepository.findByCategoryAndApprovedTrueAndEnabledTrue(category)
                .stream()
                .filter(w -> w.getPincode() != null && w.getPincode().equals(pincode))
                .limit(10)
                .collect(java.util.stream.Collectors.toList());

        // If fewer than 10 by pincode, fall back to all in category
        if (nearby.isEmpty()) {
            nearby = workerRepository.findByCategoryAndApprovedTrueAndEnabledTrue(category)
                    .stream().limit(10).collect(java.util.stream.Collectors.toList());
        }

        ServiceRequest req = new ServiceRequest();
        req.setUser(user);
        req.setCategory(category);
        req.setProblemDescription(problemDescription);
        req.setBudget(budget);
        req.setUrgency(urgency);
        req.setPincode(pincode);
        if (latitude != null) req.setLatitude(latitude);
        if (longitude != null) req.setLongitude(longitude);
        req.setNotifiedWorkers(nearby);
        serviceRequestRepository.save(req);

        // Send quotation notifications to workers
        quotationService.sendQuotationRequests(req, nearby);

        return Map.of("status", "success", "message", "Request sent to " + nearby.size() + " workers!", "requestId",
                req.getId());
    }

    @GetMapping("/my-quotes")
    public String myQuotes(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null)
            return "redirect:/login";

        model.addAttribute("requests", serviceRequestRepository.findByUserOrderByCreatedAtDesc(user));
        model.addAttribute("username", user.getName());
        return "user/my-quotes";
    }

    // ================= QUOTATION RESPONSES =================
    @GetMapping("/quotation-responses")
    public String quotationResponses(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null)
            return "redirect:/login";

        model.addAttribute("user", user);
        return "user/quotation_responses";
    }

    @GetMapping("/quotation-chat")
    public String quotationChat(@RequestParam Long chatId, Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        model.addAttribute("chatId", chatId);
        return "common/quotation_chat";
    }

    @PostMapping("/accept-quote")
    @ResponseBody
    public Map<String, Object> acceptQuote(@RequestParam Long quoteId, Principal principal) {
        if (principal == null)
            return Map.of("status", "error", "message", "Unauthorized");
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null)
            return Map.of("status", "error", "message", "Unauthorized");

        ServiceQuote quote = serviceQuoteRepository.findById(quoteId).orElse(null);
        if (quote == null)
            return Map.of("status", "error", "message", "Quote not found");
        if (!quote.getServiceRequest().getUser().getId().equals(user.getId()))
            return Map.of("status", "error", "message", "Unauthorized");

        // Convert quote to a booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setWorker(quote.getWorker());
        booking.setBookingDate(java.time.LocalDate.now().plusDays(1));
        booking.setBookingTime(java.time.LocalTime.of(10, 0));
        booking.setDescription(quote.getServiceRequest().getProblemDescription());
        booking.setPincode(quote.getServiceRequest().getPincode());
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setOriginalAmount(quote.getAmount());
        booking.setDiscountedAmount(quote.getAmount());
        bookingRepository.save(booking);

        // Mark quote as accepted, request as closed
        quote.setStatus(ServiceQuote.QuoteStatus.ACCEPTED);
        serviceQuoteRepository.save(quote);
        ServiceRequest req = quote.getServiceRequest();
        req.setStatus(ServiceRequest.RequestStatus.CLOSED);
        serviceRequestRepository.save(req);

        return Map.of("status", "success", "message", "Quote accepted! Booking created.", "bookingId", booking.getId());
    }

    // ================= DASHBOARD =================
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        Optional<User> optionalUser = userRepository.findByEmail(principal.getName());
        if (optionalUser.isEmpty())
            return "redirect:/login";

        User user = optionalUser.get();

        model.addAttribute("username", user.getName());
        model.addAttribute("userMobile", user.getMobile());
        model.addAttribute("totalBookings", bookingRepository.countByUser(user));
        model.addAttribute("completedJobs",
                bookingRepository.countByUserAndStatus(user, Booking.BookingStatus.COMPLETED));
        model.addAttribute("pendingRequests",
                bookingRepository.countByUserAndStatus(user, Booking.BookingStatus.PENDING));
        model.addAttribute("activeServices", workerRepository.findByApprovedTrueAndEnabledTrue().size());
        model.addAttribute("workers", workerRepository.findByApprovedTrueAndEnabledTrue());

        return "user/dashboard";
    }

    // ================= ACTIVE SERVICES =================
    @GetMapping("/active-services")
    public String activeServices(Model model) {
        var categories = serviceCategoryRepository.findAll();
        var workerCounts = new java.util.HashMap<String, Long>();

        for (var category : categories) {
            workerCounts.put(category.getName(), workerRepository.countAvailableByCategory(category.getName()));
        }

        model.addAttribute("serviceCategories", categories);
        model.addAttribute("workerCounts", workerCounts);
        return "user/active-services";
    }

    @GetMapping("/active-services/results")
    public String activeServicesResults(@RequestParam("category") String category, Model model) {
        model.addAttribute("workers", workerRepository.findByCategoryAndApprovedTrueAndEnabledTrue(category));
        model.addAttribute("category", category);
        model.addAttribute("workerCount", workerRepository.countAvailableByCategory(category));
        return "user/active-services-results";
    }

    @PostMapping("/active-services/filter-by-location")
    @ResponseBody
    public List<Worker> filterWorkersByLocation(@RequestBody Map<String, Object> locData) {
        double userLat = ((Number) locData.get("lat")).doubleValue();
        double userLng = ((Number) locData.get("lng")).doubleValue();
        String category = (String) locData.get("category");
        return workerRepository.findNearbyWorkers(category, userLat, userLng, 50.0);
    }

    // ================= HIRE PAGE =================
    @GetMapping("/hire/{id}")
    public String showHirePage(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        Worker worker = workerRepository.findById(id).orElse(null);
        if (worker == null)
            return "redirect:/user/active-services";

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null)
            return "redirect:/login";

        // Check for rebooking discount
        Booking dummyBooking = new Booking();
        dummyBooking.setUser(user);
        dummyBooking.setWorker(worker);
        paymentService.calculateAmount(dummyBooking);

        model.addAttribute("worker", worker);
        model.addAttribute("user", user);
        model.addAttribute("rebookingDiscountApplied", dummyBooking.getAppliedDiscount() > 0);
        model.addAttribute("originalAmount", dummyBooking.getOriginalAmount());
        model.addAttribute("discountedAmount", dummyBooking.getDiscountedAmount());
        return "user/hire-worker";
    }

    // ================= BOOK WORKER =================
    @PostMapping("/book-worker")
    public String bookWorker(
            @RequestParam Long workerId,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam(required = false) String address,
            @RequestParam String description,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String pincode,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null)
            return "redirect:/login";

        try {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            Worker worker = workerRepository.findById(workerId).orElse(null);

            if (user == null || worker == null) {
                redirectAttributes.addFlashAttribute("error", "Error creating booking.");
                return "redirect:/user/dashboard";
            }

            Booking booking = new Booking();
            booking.setUser(user);
            booking.setWorker(worker);
            booking.setBookingDate(java.time.LocalDate.parse(date));
            booking.setBookingTime(java.time.LocalTime.parse(time));
            booking.setAddress((address != null && !address.isBlank()) ? address : (city + ", " + state));
            booking.setDescription(description);
            booking.setLatitude(latitude);
            booking.setLongitude(longitude);
            booking.setCity(city);
            booking.setState(state);
            booking.setPincode(pincode);
            booking.setStatus(Booking.BookingStatus.PENDING);

            // Calculate amount and discount
            paymentService.calculateAmount(booking);

            String otp = notificationService.generateOtp();
            bookingRepository.save(booking);
            notificationService.notifyWorkerNewBooking(booking, otp);
            notificationService.notifyUserBookingPlaced(booking, otp);

            redirectAttributes.addFlashAttribute("success", "Booking confirmed!");
            return "redirect:/user/booking-success";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/user/hire/" + workerId;
        }
    }

    // ================= MY BOOKINGS =================
    @GetMapping("/bookings")
    public String myBookings(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null)
            return "redirect:/login";

        List<Booking> bookings = bookingRepository.findByUserOrderByCreatedAtDesc(user);
        java.util.Set<Long> ratedBookingIds = new java.util.HashSet<>();
        java.util.Set<Long> paidBookingIds = new java.util.HashSet<>();
        for (Booking b : bookings) {
            if (ratingRepository.findByBooking(b).isPresent()) {
                ratedBookingIds.add(b.getId());
            }
            paymentService.getPaymentForBooking(b).ifPresent(p -> {
                if (p.getStatus() == Payment.PaymentStatus.SUCCESS) {
                    paidBookingIds.add(b.getId());
                }
            });
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("ratedBookingIds", ratedBookingIds);
        model.addAttribute("paidBookingIds", paidBookingIds);
        model.addAttribute("username", user.getName());
        return "user/bookings";
    }

    @GetMapping("/booking-success")
    public String bookingSuccess() {
        return "user/booking-success";
    }

    // ================= CHAT =================
    @GetMapping("/chat")
    public String chat(@RequestParam Long bookingId, Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null)
            return "redirect:/login";

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || !booking.getUser().getId().equals(user.getId())) {
            return "redirect:/user/bookings";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("username", user.getName());
        return "user/chat";
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
        report.setSenderRole(Role.USER);
        report.setTargetEmail(targetEmail);
        report.setTargetRole(Role.valueOf(targetRole));

        if (bookingId != null) {
            bookingRepository.findById(bookingId).ifPresent(report::setBooking);
        }

        report.setReason(reason);
        report.setDescription(description);
        reportRepository.save(report);

        redirectAttributes.addFlashAttribute("successMsg", "Your report has been submitted.");
        return "redirect:/user/bookings";
    }

    // ================= RATE WORKER =================
    @GetMapping("/rate-worker")
    public String showRateWorkerPage(@RequestParam Long bookingId, Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || !booking.getUser().getEmail().equals(principal.getName()))
            return "redirect:/user/dashboard";

        // Check if already rated
        if (ratingRepository.findByBooking(booking).isPresent()) {
            return "redirect:/user/bookings?error=already_rated";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("worker", booking.getWorker());
        return "user/rate-worker";
    }

    @PostMapping("/submit-rating")
    public String submitRating(
            @RequestParam Long bookingId,
            @RequestParam int stars,
            @RequestParam(required = false) String review,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null)
            return "redirect:/login";
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null)
            return "redirect:/user/dashboard";

        // Prevent duplicate ratings
        if (ratingRepository.findByBooking(booking).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "You have already rated this booking.");
            return "redirect:/user/bookings";
        }

        try {
            Rating rating = new Rating();
            rating.setUser(booking.getUser());
            rating.setWorker(booking.getWorker());
            rating.setBooking(booking);
            rating.setStars(stars);
            rating.setReview(review);
            ratingRepository.save(rating);

            // Update Worker's overall rating
            Worker worker = booking.getWorker();
            Double avgRating = ratingRepository.getAverageRatingForWorker(worker);
            if (avgRating != null) {
                worker.setRating(avgRating);
                workerRepository.save(worker);
            }

            redirectAttributes.addFlashAttribute("successMsg", "Feedback submitted! Thank you.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error saving your rating. Please try again.");
        }

        return "redirect:/user/bookings";
    }

    // ================= LIVE TRACKING =================
    @GetMapping("/track-worker/{bookingId}")
    public String trackWorker(@PathVariable Long bookingId, Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || !booking.getUser().getEmail().equals(principal.getName())) {
            return "redirect:/user/dashboard";
        }
        model.addAttribute("booking", booking);
        model.addAttribute("worker", booking.getWorker());
        return "user/track-worker";
    }

    @GetMapping("/api/worker-location/{workerId}")
    @ResponseBody
    public Map<String, Object> getWorkerLocation(@PathVariable Long workerId) {
        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker != null && worker.getLatitude() != null && worker.getLongitude() != null) {
            return Map.of("status", "success", "lat", worker.getLatitude(), "lng", worker.getLongitude());
        }
        return Map.of("status", "error", "message", "Location unavailable");
    }

    // ================= USER PROFILE =================
    @GetMapping("/profile")
    public String showProfile(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        Optional<User> optionalUser = userRepository.findByEmail(principal.getName());
        if (optionalUser.isEmpty())
            return "redirect:/login";

        User user = optionalUser.get();
        model.addAttribute("user", user);
        return "user/profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(
            @RequestParam String name,
            @RequestParam String mobile,
            @RequestParam String addressLine,
            @RequestParam String city,
            @RequestParam String state,
            @RequestParam String pincode,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null)
            return "redirect:/login";

        try {
            Optional<User> optionalUser = userRepository.findByEmail(principal.getName());
            if (optionalUser.isEmpty())
                return "redirect:/login";

            User user = optionalUser.get();

            // Update user profile fields
            user.setName(name);
            user.setMobile(mobile);
            user.setAddressLine(addressLine);
            user.setCity(city);
            user.setState(state);
            user.setPincode(pincode);

            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error updating profile. Please try again.");
        }

        return "redirect:/user/profile";
    }

    @GetMapping("/notifications")
    public String showNotifications(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        Optional<User> optionalUser = userRepository.findByEmail(principal.getName());
        if (optionalUser.isEmpty())
            return "redirect:/login";

        User user = optionalUser.get();
        model.addAttribute("notifications", notificationRepository.findByUserOrderByCreatedAtDesc(user));

        // Mark as read
        notificationService.markAllReadForUser(user);

        return "user/notifications";
    }
}
