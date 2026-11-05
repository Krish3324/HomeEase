package com.homeease.homeease.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.service.AuthService;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // ======================
    @PostMapping("/register-user")
    public String registerUser(
            @ModelAttribute User user, 
            @RequestParam("profileImageFile") MultipartFile profileImage,
            Model model) {

        try {
            if (profileImage == null || profileImage.isEmpty()) {
                model.addAttribute("error", "Profile photo is required!");
                return "register-user";
            }
            if (profileImage.getSize() > 3 * 1024 * 1024) {
                model.addAttribute("error", "Profile photo size must be less than 3MB.");
                return "register-user";
            }
            String contentType = profileImage.getContentType();
            if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/jpg"))) {
                model.addAttribute("error", "Only JPG and PNG images are allowed for profile photo.");
                return "register-user";
            }

            String password = user.getPassword();
            String pwdRegex = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,16}$";
            if (password == null || !password.matches(pwdRegex)) {
                model.addAttribute("error", "Password must be 8-16 characters long, include at least one uppercase letter, one number, and one special character.");
                return "register-user";
            }

            String result = authService.registerUser(user, profileImage);

            if (result.contains("already")) {
                model.addAttribute("error", result);
                return "register-user";
            }

            model.addAttribute("success", result);
            return "login";
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Something went wrong. Please try again.");
            return "register-user";
        }
    }

    // ======================
    // WORKER REGISTER WITH IMAGE UPLOAD
    // ======================
    @PostMapping("/register-worker")
    public String registerWorker(
            @ModelAttribute Worker worker,
            @RequestParam("verificationIdImage") MultipartFile file,
            @RequestParam("profileImageFile") MultipartFile profileImage,
            Model model) {

        try {

            // 🔴 Check if file missing
            if (file == null || file.isEmpty()) {
                model.addAttribute("error", "Verification ID image is required!");
                return "register-worker";
            }
            if (profileImage == null || profileImage.isEmpty()) {
                model.addAttribute("error", "Profile photo is required!");
                return "register-worker";
            }

            // 🔴 Check file size (3MB limit)
            if (file.getSize() > 3 * 1024 * 1024 || profileImage.getSize() > 3 * 1024 * 1024) {
                model.addAttribute("error", "File size must be less than 3MB.");
                return "register-worker";
            }

            // 🔴 Optional: Validate file type
            String contentType = file.getContentType();
            String profileContentType = profileImage.getContentType();
            
            boolean validIdFile = contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/jpg"));
            boolean validProfileFile = profileContentType != null && (profileContentType.equals("image/jpeg") || profileContentType.equals("image/png") || profileContentType.equals("image/jpg"));
            
            if (!validIdFile || !validProfileFile) {
                model.addAttribute("error", "Only JPG and PNG images are allowed.");
                return "register-worker";
            }

            String password = worker.getPassword();
            String pwdRegex = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,16}$";
            if (password == null || !password.matches(pwdRegex)) {
                model.addAttribute("error", "Password must be 8-16 characters long, include at least one uppercase letter, one number, and one special character.");
                return "register-worker";
            }

            String result = authService.registerWorker(worker, file, profileImage);

            if (result.contains("already")) {
                model.addAttribute("error", result);
                return "register-worker";
            }

            model.addAttribute("success", result);
            return "register-worker";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Something went wrong. Please try again.");
            return "register-worker";
        }
    }

    // ======================
    // EMAIL VERIFY
    // ======================
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token) {

        String result = authService.verifyEmail(token);

        if (result.contains("verified")) {
            return "redirect:/login";
        }

        return "redirect:/error";
    }

    // ======================
// FORGOT PASSWORD - SHOW FORM
// ======================
@GetMapping("/forgot-password")
public String forgotPasswordPage() {
    return "forgot-password";
}

// ======================
// FORGOT PASSWORD - HANDLE EMAIL SUBMIT
// ======================
@PostMapping("/forgot-password")
public String forgotPasswordSubmit(
        @RequestParam("email") String email,
        Model model) {

    String result = authService.forgotPassword(email);

    if ("success".equals(result)) {
        model.addAttribute("success",
                "Password reset link sent! Please check your email.");
    } else {
        model.addAttribute("error",
                "No account found with that email address.");
    }

    return "forgot-password";
}

// ======================
// RESET PASSWORD - SHOW FORM
// ======================
@GetMapping("/reset-password2")
public String resetPasswordPage(
        @RequestParam("token") String token,
        Model model) {

    // Validate token exists
    model.addAttribute("token", token);
    return "reset-password2";
}

// ======================
// RESET PASSWORD - HANDLE NEW PASSWORD SUBMIT
// ======================
@PostMapping("/reset-password2")
public String resetPasswordSubmit(
        @RequestParam("token") String token,
        @RequestParam("password") String password,
        @RequestParam("confirmPassword") String confirmPassword,
        Model model) {

    if (!password.equals(confirmPassword)) {
        model.addAttribute("error", "Passwords do not match!");
        model.addAttribute("token", token);
        return "reset-password2";
    }

    if (password.length() < 6) {
        model.addAttribute("error", "Password must be at least 6 characters!");
        model.addAttribute("token", token);
        return "reset-password2";
    }

    String result = authService.resetPassword(token, password);

    if ("success".equals(result)) {
        model.addAttribute("success",
                "Password reset successful! You can now login.");
        return "reset-password2";
    } else {
        model.addAttribute("error",
                "Invalid or expired reset link. Please try again.");
        model.addAttribute("token", token);
        return "reset-password2";
    }
}
}
