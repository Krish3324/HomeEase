package com.homeease.homeease.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.repository.UserRepository;
import com.homeease.homeease.repository.WorkerRepository;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    // ======================
    // REGISTER USER
    // ======================
    public String registerUser(User user, MultipartFile profileImage) {

        try {
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                return "Email already registered as User!";
            }

            if (workerRepository.findByEmail(user.getEmail()).isPresent()) {
                return "Email already registered as Worker!";
            }

            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setEnabled(false);
            user.setEmailVer(0);
            
            String publicId = generateUserId("USR");
            user.setPublicUserId(publicId);

            // ======================
            // CREATE FOLDER STRUCTURE
            // ======================
            String baseDir = System.getProperty("user.dir") + "/ProfilePhoto/User";
            String userFolderPath = baseDir + "/" + publicId;

            File userFolder = new File(userFolderPath);
            if (!userFolder.exists()) {
                userFolder.mkdirs();
            }

            String originalFileName = profileImage.getOriginalFilename();
            if (originalFileName == null || !originalFileName.contains(".")) {
                return "Invalid file format for profile photo!";
            }

            String extension = originalFileName
                    .substring(originalFileName.lastIndexOf("."))
                    .toLowerCase();

            String fileName = "profile" + extension;
            Path path = Paths.get(userFolderPath, fileName);
            Files.write(path, profileImage.getBytes());

            // Save image path in DB
            user.setProfileImage("/ProfilePhoto/User/" + publicId + "/" + fileName);

            String token = UUID.randomUUID().toString();
            user.setVerificationToken(token);

            userRepository.save(user);

            emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);

            return "Verification email sent! Please check your email.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Something went wrong while registering user.";
        }
    }

    // ======================
    // REGISTER WORKER WITH IMAGE
    // ======================
    public String registerWorker(Worker worker, MultipartFile file, MultipartFile profileImage) {

        try {

            if (userRepository.findByEmail(worker.getEmail()).isPresent()) {
                return "Email already registered as User!";
            }

            if (workerRepository.findByEmail(worker.getEmail()).isPresent()) {
                return "Email already registered as Worker!";
            }

            // 🔐 Password encode
            worker.setPassword(passwordEncoder.encode(worker.getPassword()));
            worker.setEnabled(false);
            worker.setApproved(false);
            worker.setEmailVer(0);

            // 🔥 Generate 12-char unique ID
            String publicId = generateUserId("WRK");
            worker.setPublicUserId(publicId);

            // ======================
            // CREATE FOLDER STRUCTURE FOR ID
            // ======================
            String baseDir = System.getProperty("user.dir") + "/Worker_ID_IMG";
            String workerFolderPath = baseDir + "/" + publicId;

            File workerFolder = new File(workerFolderPath);
            if (!workerFolder.exists()) {
                workerFolder.mkdirs();
            }

            // Get file extension// Get original filename safely
            String originalFileName = file.getOriginalFilename();

            if (originalFileName == null || !originalFileName.contains(".")) {
                return "Invalid file format!";
            }

            String extension = originalFileName
                    .substring(originalFileName.lastIndexOf("."))
                    .toLowerCase();

            if (!extension.equals(".jpg")
                    && !extension.equals(".jpeg")
                    && !extension.equals(".png")) {

                return "Only JPG and PNG images are allowed!";
            }
            String fileName = "verification_id" + extension;

            Path path = Paths.get(workerFolderPath, fileName);
            Files.write(path, file.getBytes());

            // Save image path in DB
            worker.setIdImagePath(path.toString().replace("\\", "/"));

            // ======================
            // CREATE FOLDER STRUCTURE FOR PROFILE PHOTO
            // ======================
            String baseDirProfile = System.getProperty("user.dir") + "/ProfilePhoto/Worker";
            String workerProfileFolderPath = baseDirProfile + "/" + publicId;

            File workerProfileFolder = new File(workerProfileFolderPath);
            if (!workerProfileFolder.exists()) {
                workerProfileFolder.mkdirs();
            }

            String originalProfileFileName = profileImage.getOriginalFilename();
            if (originalProfileFileName == null || !originalProfileFileName.contains(".")) {
                return "Invalid file format for profile photo!";
            }

            String profileExtension = originalProfileFileName
                    .substring(originalProfileFileName.lastIndexOf("."))
                    .toLowerCase();

            String profileFileName = "profile" + profileExtension;
            Path profilePath = Paths.get(workerProfileFolderPath, profileFileName);
            Files.write(profilePath, profileImage.getBytes());

            // Save profile image path in DB
            worker.setProfileImage("/ProfilePhoto/Worker/" + publicId + "/" + profileFileName);

            // ======================
            // EMAIL TOKEN
            // ======================
            String token = UUID.randomUUID().toString();
            worker.setVerificationToken(token);

            workerRepository.save(worker);

            emailService.sendVerificationEmail(
                    worker.getEmail(),
                    worker.getName(),
                    token
            );

            return "Worker registered successfully! Please verify email.";

        } catch (Exception e) {
            e.printStackTrace();
            try {
                java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.File("error_log.txt"));
                e.printStackTrace(pw);
                pw.close();
            } catch (Exception ex) {}
            return "Something went wrong while registering worker.";
        }
    }

    // ======================
    // GENERATE UNIQUE ID (12 RANDOM CHARACTERS)
    // ======================
    private String generateUserId(String prefix) {

        String id;

        do {
            String uniquePart = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 12)
                    .toUpperCase();

            id = prefix + "-" + uniquePart;

        } while (userRepository.existsByPublicUserId(id)
                || workerRepository.existsByPublicUserId(id));

        return id;
    }

    // ======================
    // LOGIN
    // ======================
    public String login(String email, String password, String role) {

        if (role.equalsIgnoreCase("USER")) {

            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isPresent()
                    && passwordEncoder.matches(password, userOpt.get().getPassword())) {

                return "User login successful";
            } else {
                return "Invalid user credentials";
            }

        } else if (role.equalsIgnoreCase("WORKER")) {

            Optional<Worker> workerOpt = workerRepository.findByEmail(email);

            if (workerOpt.isPresent()
                    && passwordEncoder.matches(password, workerOpt.get().getPassword())) {

                return "Worker login successful";
            } else {
                return "Invalid worker credentials";
            }
        }

        return "Role not recognized";
    }

    // ======================
    // VERIFY EMAIL
    // ======================
    public String verifyEmail(String token) {

        Optional<User> userOpt = userRepository.findByVerificationToken(token);

        if (userOpt.isPresent()) {

            User user = userOpt.get();
            user.setEnabled(true);
            user.setEmailVer(1);
            user.setVerificationToken(null);

            userRepository.save(user);

            return "User email verified successfully!";
        }

        Optional<Worker> workerOpt = workerRepository.findByVerificationToken(token);

        if (workerOpt.isPresent()) {

            Worker worker = workerOpt.get();
            worker.setEnabled(true);
            worker.setEmailVer(1);
            worker.setVerificationToken(null);

            workerRepository.save(worker);

            return "Worker email verified successfully! Wait for admin approval.";
        }

        return "Invalid or expired token!";
    }

// ======================
// FORGOT PASSWORD - SEND RESET EMAIL
// ======================
    public String forgotPassword(String email) {

        // Check in users
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String token = UUID.randomUUID().toString();
            user.setVerificationToken(token);
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
            return "success";
        }

        // Check in workers
        Optional<Worker> workerOpt = workerRepository.findByEmail(email);
        if (workerOpt.isPresent()) {
            Worker worker = workerOpt.get();
            String token = UUID.randomUUID().toString();
            worker.setVerificationToken(token);
            workerRepository.save(worker);
            emailService.sendPasswordResetEmail(worker.getEmail(), worker.getName(), token);
            return "success";
        }

        return "not_found";
    }

// ======================
// RESET PASSWORD - SAVE NEW PASSWORD
// ======================
    public String resetPassword(String token, String newPassword) {

        // Check in users
        Optional<User> userOpt = userRepository.findByVerificationToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setVerificationToken(null);
            userRepository.save(user);
            return "success";
        }

        // Check in workers
        Optional<Worker> workerOpt = workerRepository.findByVerificationToken(token);
        if (workerOpt.isPresent()) {
            Worker worker = workerOpt.get();
            worker.setPassword(passwordEncoder.encode(newPassword));
            worker.setVerificationToken(null);
            workerRepository.save(worker);
            return "success";
        }

        return "invalid";
    }

}
