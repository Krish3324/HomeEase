package com.homeease.homeease.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Inject base URL from application.properties
    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String email, String name, String token) {

        // Build verification link correctly
        String verifyLink = baseUrl + "/auth/verify-email?token=" + token;

        String subject = "Verify your HomeEase account";

        String htmlContent = "<!DOCTYPE html>"
                + "<html>"
                + "<body style='font-family: Arial, sans-serif; background:#f4f6f8; padding:20px'>"
                + "<div style='max-width:600px;margin:auto;background:#ffffff;padding:30px;border-radius:8px'>"
                + "<h2 style='color:#333'>Hello " + name + ",</h2>"
                + "<p style='color:#555;font-size:15px'>"
                + "Thank you for registering with <b>HomeEase</b>. Please verify your email to activate your account."
                + "</p>"
                + "<div style='text-align:center;margin:30px 0'>"
                + "<a href='" + verifyLink + "' "
                + "style='background:#FFFF00;color:black;padding:12px 25px;"
                + "text-decoration:none;border-radius:5px;font-size:16px'>"
                + "Verify Email</a>"
                + "</div>"
                + "<p style='font-size:13px;color:#777'>"
                + "If the button does not work, copy and paste this link into your browser:<br>"
                + "<span style='word-break:break-all'>" + verifyLink + "</span>"
                + "</p>"
                + "<hr style='margin-top:30px'>"
                + "<p style='font-size:12px;color:#999;text-align:center'>"
                + "HomeEase • This is an automated email, please do not reply"
                + "</p>"
                + "</div>"
                + "</body>"
                + "</html>";
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("HomeEase <homeease19@gmail.com>");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML content

            mailSender.send(message);

        } catch (Exception e) {
            System.out.println("Email Failed: " + e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String email, String name, String token) {
        String resetlink = baseUrl + "/auth/reset-password2?token=" + token;
        String subject = "Reset Your Password";

        String htmlContent = "<!DOCTYPE html>"
                + "<html><body style='font-family: Arial, sans-serif; background:#f4f6f8; padding:20px'>"
                + "<div style='max-width:600px;margin:auto;background:#ffffff;padding:30px;border-radius:8px'>"
                + "<h2 style='color:#333'>Hello " + name + ",</h2>"
                + "<p style='color:#555;font-size:15px'>"
                + "We received a request to reset your <b>HomeEase</b> password."
                + "</p>"
                + "<div style='text-align:center;margin:30px 0'>"
                + "<a href='" + resetlink + "' "
                + "style='background:#FFFF00;color:black;padding:12px 25px;"
                + "text-decoration:none;border-radius:5px;font-size:16px'>"
                + "Reset Password</a>"
                + "</div>"
                + "<p style='font-size:13px;color:#777'>"
                + "If you did not request this, ignore this email.<br>"
                + "Link: <span style='word-break:break-all'>" + resetlink + "</span>"
                + "</p>"
                + "<hr style='margin-top:30px'>"
                + "<p style='font-size:12px;color:#999;text-align:center'>"
                + "HomeEase • This is an automated email, please do not reply"
                + "</p>"
                + "</div></body></html>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("HomeEase <homeease19@gmail.com>");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("Password reset email failed: " + e.getMessage());
        }
    }

    public void sendRatingEmail(String email, String userName, String workerName, Long bookingId) {
        String ratingLink = baseUrl + "/user/rate-worker?bookingId=" + bookingId;
        String subject = "Rate Your Service by " + workerName;

        String htmlContent = "<!DOCTYPE html>"
                + "<html><body style='font-family: Arial, sans-serif; background:#f4f6f8; padding:20px'>"
                + "<div style='max-width:600px;margin:auto;background:#ffffff;padding:30px;border-radius:8px'>"
                + "<h2 style='color:#333'>Hello " + userName + ",</h2>"
                + "<p style='color:#555;font-size:15px'>"
                + workerName + " has marked your booked service as completed. "
                + "Please take a moment to rate the service!"
                + "</p>"
                + "<div style='text-align:center;margin:30px 0'>"
                + "<a href='" + ratingLink + "' "
                + "style='background:#FFFF00;color:black;padding:12px 25px;"
                + "text-decoration:none;border-radius:5px;font-size:16px'>"
                + "Rate Worker</a>"
                + "</div>"
                + "<p style='font-size:13px;color:#777'>"
                + "Link not working? Copy this into your browser:<br>"
                + "<span style='word-break:break-all'>" + ratingLink + "</span>"
                + "</p>"
                + "<hr style='margin-top:30px'>"
                + "<p style='font-size:12px;color:#999;text-align:center'>"
                + "HomeEase • This is an automated email, please do not reply"
                + "</p>"
                + "</div></body></html>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("HomeEase <homeease19@gmail.com>");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("Rating email failed: " + e.getMessage());
        }
    }

    // ─── ADD THIS METHOD to your existing EmailService.java ───

    public void sendOtpEmail(String email, String userName, String workerName,
            String otp, String bookingDate, String bookingTime) {

        String subject = "HomeEase — Booking Confirmed! Your OTP: " + otp;

        String htmlContent = "<!DOCTYPE html>"
                + "<html><body style='font-family: Arial, sans-serif; background:#f4f6f8; padding:20px'>"
                + "<div style='max-width:600px;margin:auto;background:#ffffff;padding:30px;border-radius:8px'>"
                + "<h2 style='color:#333'>Hello " + userName + ",</h2>"
                + "<p style='color:#555;font-size:15px'>"
                + "<b>" + workerName + "</b> has <span style='color:green;font-weight:bold;'>accepted</span>"
                + " your booking on <b>" + bookingDate + "</b> at <b>" + bookingTime + "</b>."
                + "</p>"
                + "<p style='color:#555;font-size:15px;margin-top:15px;'>"
                + "Please share this OTP with the worker when they arrive at your home to confirm their presence:"
                + "</p>"
                + "<div style='text-align:center;margin:30px 0'>"
                + "<div style='display:inline-block;background:#1c1c1c;color:#FFE079;"
                + "font-size:42px;font-weight:800;padding:20px 40px;"
                + "border-radius:12px;letter-spacing:10px;'>"
                + otp
                + "</div>"
                + "</div>"
                + "<p style='font-size:13px;color:#e74c3c;text-align:center;'>"
                + "⚠ Do NOT share this OTP with anyone other than your worker."
                + "</p>"
                + "<hr style='margin-top:30px'>"
                + "<p style='font-size:12px;color:#999;text-align:center'>"
                + "HomeEase • This is an automated email, please do not reply"
                + "</p>"
                + "</div></body></html>";

        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    message, true);
            helper.setFrom("HomeEase <homeease19@gmail.com>");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("OTP email failed: " + e.getMessage());
        }
    }

    public void sendWorkerApprovedEmail(String email, String name) {
        String subject = "HomeEase — Your Worker Account Approved!";
        String htmlContent = "<!DOCTYPE html>"
                + "<html><body style='font-family: Arial, sans-serif; background:#f4f6f8; padding:20px'>"
                + "<div style='max-width:600px;margin:auto;background:#ffffff;padding:30px;border-radius:8px'>"
                + "<h2 style='color:#22c55e'>Congratulations " + name + "!</h2>"
                + "<p style='color:#333;font-size:16px;line-height:1.5;'>"
                + "We are thrilled to inform you that your worker profile and verification ID have been reviewed and <b>approved</b> by the administrator."
                + "</p>"
                + "<p style='color:#555;font-size:15px;line-height:1.5;'>"
                + "You are now ready to start working on the HomeEase platform. You can log in to your dashboard to set your availability, respond to client quote requests, and begin earning!"
                + "</p>"
                + "<div style='text-align:center;margin:30px 0'>"
                + "<a href='" + baseUrl + "/login' "
                + "style='background:#ffdb15;color:#111;padding:12px 30px;"
                + "text-decoration:none;border-radius:6px;font-size:16px;font-weight:bold;display:inline-block;box-shadow:0 4px 6px rgba(0,0,0,0.1)'>"
                + "Go to Dashboard</a>"
                + "</div>"
                + "<hr style='margin-top:30px;border:none;border-top:1px solid #eee;'>"
                + "<p style='font-size:12px;color:#999;text-align:center'>"
                + "HomeEase • This is an automated email, please do not reply"
                + "</p>"
                + "</div></body></html>";

        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    message, true);
            helper.setFrom("HomeEase <homeease19@gmail.com>");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("Worker approved email failed: " + e.getMessage());
        }
    }

    public void sendWorkerDeclinedEmail(String email, String name) {
        String subject = "HomeEase — Application Status Update";
        String htmlContent = "<!DOCTYPE html>"
                + "<html><body style='font-family: Arial, sans-serif; background:#f4f6f8; padding:20px'>"
                + "<div style='max-width:600px;margin:auto;background:#ffffff;padding:30px;border-radius:8px'>"
                + "<h2 style='color:#ef4444'>Hello " + name + ",</h2>"
                + "<p style='color:#333;font-size:16px;line-height:1.5;'>"
                + "Thank you for your interest in joining HomeEase. We have completed the review of your worker registration request."
                + "</p>"
                + "<p style='color:#e74c3c;font-size:15px;font-weight:bold;margin:20px 0;padding:12px;background:#fdf2f2;border-left:4px solid #ef4444;border-radius:4px;'>"
                + "Unfortunately, your application has been declined because your uploaded verification ID was found to be invalid."
                + "</p>"
                + "<p style='color:#555;font-size:15px;line-height:1.5;'>"
                + "To resolve this, please register again ensuring that all details are accurate and you upload a clear, valid official identification document (such as Aadhar, PAN, Voter ID, or Driver's license)."
                + "</p>"
                + "<div style='text-align:center;margin:30px 0'>"
                + "<a href='" + baseUrl + "/register-worker' "
                + "style='background:#ef4444;color:white;padding:12px 30px;"
                + "text-decoration:none;border-radius:6px;font-size:16px;font-weight:bold;display:inline-block;box-shadow:0 4px 6px rgba(0,0,0,0.1)'>"
                + "Register Again</a>"
                + "</div>"
                + "<hr style='margin-top:30px;border:none;border-top:1px solid #eee;'>"
                + "<p style='font-size:12px;color:#999;text-align:center'>"
                + "HomeEase • This is an automated email, please do not reply"
                + "</p>"
                + "</div></body></html>";

        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    message, true);
            helper.setFrom("HomeEase <homeease19@gmail.com>");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("Worker declined email failed: " + e.getMessage());
        }
    }

}
