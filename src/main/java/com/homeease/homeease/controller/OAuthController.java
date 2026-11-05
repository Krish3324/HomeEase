package com.homeease.homeease.controller;

import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.repository.UserRepository;
import com.homeease.homeease.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDate;

@Controller
public class OAuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // ==================== USER ====================

    @GetMapping("/user/complete-profile")
    public String userCompleteProfilePage(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        return "user/user-after-first-login";
    }

    @PostMapping("/user/complete-profile")
    public String userCompleteProfile(
            Principal principal,
            @RequestParam("mobile") String mobile,
            @RequestParam("addressLine") String addressLine,
            @RequestParam("city") String city,
            @RequestParam("state") String state,
            @RequestParam("pincode") String pincode,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            RedirectAttributes redirectAttributes) {

        if (principal == null) return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        // Validate passwords
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
            return "redirect:/user/complete-profile";
        }

        user.setMobile(mobile);
        user.setAddressLine(addressLine);
        user.setCity(city);
        user.setState(state);
        user.setPincode(pincode);
        user.setPassword(passwordEncoder.encode(password));

        // Save profile image
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String folderPath = System.getProperty("user.dir")
                        + "/User-profile_image/" + user.getPublicUserId();
                File folder = new File(folderPath);
                if (!folder.exists()) folder.mkdirs();

                String original = profileImage.getOriginalFilename();
                String ext = original.substring(original.lastIndexOf(".")).toLowerCase();
                Path path = Paths.get(folderPath, "profile" + ext);
                Files.write(path, profileImage.getBytes());
                user.setProfileImage(path.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        userRepository.save(user);
        return "redirect:/user/dashboard";
    }

    // ==================== WORKER ====================

    @GetMapping("/worker/complete-profile")
    public String workerCompleteProfilePage(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null) return "redirect:/login";
        model.addAttribute("worker", worker);
        return "worker/worker-after-first-login";
    }

    @PostMapping("/worker/complete-profile")
    public String workerCompleteProfile(
            Principal principal,
            @RequestParam("mobile") String mobile,
            @RequestParam("city") String city,
            @RequestParam("state") String state,
            @RequestParam("pincode") String pincode,
            @RequestParam("category") String category,
            @RequestParam("experience") int experience,
            @RequestParam("minCharge") Double minCharge,
            @RequestParam("maxCharge") Double maxCharge,
            @RequestParam("bio") String bio,
            @RequestParam("dob") String dob,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "idImage", required = false) MultipartFile idImage,
            RedirectAttributes redirectAttributes) {

        if (principal == null) return "redirect:/login";
        Worker worker = workerRepository.findByEmail(principal.getName()).orElse(null);
        if (worker == null) return "redirect:/login";

        // Validate passwords
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
            return "redirect:/worker/complete-profile";
        }

        worker.setMobile(mobile);
        worker.setCity(city);
        worker.setState(state);
        worker.setPincode(pincode);
        worker.setCategory(category);
        worker.setExperience(experience);
        worker.setMinCharge(minCharge);
        worker.setMaxCharge(maxCharge);
        worker.setBio(bio);
        worker.setPassword(passwordEncoder.encode(password));

        if (dob != null && !dob.isBlank()) {
            worker.setDob(LocalDate.parse(dob));
        }

        // Save profile image
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String folderPath = System.getProperty("user.dir")
                        + "/Worker-profile_image/" + worker.getPublicUserId();
                File folder = new File(folderPath);
                if (!folder.exists()) folder.mkdirs();

                String original = profileImage.getOriginalFilename();
                String ext = original.substring(original.lastIndexOf(".")).toLowerCase();
                Path path = Paths.get(folderPath, "profile" + ext);
                Files.write(path, profileImage.getBytes());
                worker.setProfileImage(path.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Save ID image
        if (idImage != null && !idImage.isEmpty()) {
            try {
                String folderPath = System.getProperty("user.dir")
                        + "/Worker_ID_IMG/" + worker.getPublicUserId();
                File folder = new File(folderPath);
                if (!folder.exists()) folder.mkdirs();

                String original = idImage.getOriginalFilename();
                String ext = original.substring(original.lastIndexOf(".")).toLowerCase();
                Path path = Paths.get(folderPath, "verification_id" + ext);
                Files.write(path, idImage.getBytes());
                worker.setIdImagePath(path.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        workerRepository.save(worker);
        return "redirect:/login?error=pending_approval";
    }
}