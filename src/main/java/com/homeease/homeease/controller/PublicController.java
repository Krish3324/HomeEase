package com.homeease.homeease.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.homeease.homeease.model.Worker;
import com.homeease.homeease.repository.WorkerRepository;
import com.homeease.homeease.repository.RatingRepository;

@Controller
public class PublicController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/services")
    public String servicesPage() {
        return "services";
    }

    @GetMapping("/login")
    public String loginPage(org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            boolean isWorker = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_WORKER"));
            if (isWorker) {
                return "redirect:/worker/dashboard";
            } else {
                return "redirect:/user/dashboard";
            }
        }
        return "login";
    }

    @GetMapping("/register-user")
    public String registerUserPage() {
        return "register-user";
    }

    @GetMapping("/register-worker")
    public String registerWorkerPage() {
        return "register-worker";
    }

    @GetMapping("/explore_service")
    public String exploreservice() {
        return "explore_service";
    }

    // ================= WORKER PROFILE PAGE =================
    @GetMapping("/worker-profile/{id}")
    public String workerProfile(@PathVariable Long id, Model model) {
        Worker worker = workerRepository.findById(id).orElse(null);
        if (worker == null)
            return "redirect:/user/active-services";

        model.addAttribute("worker", worker);
        model.addAttribute("reviews", ratingRepository.findByWorkerOrderByCreatedAtDesc(worker));

        return "worker-profile"; // worker-profile.html
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}