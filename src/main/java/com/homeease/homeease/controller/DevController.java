package com.homeease.homeease.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.model.Role;
import com.homeease.homeease.repository.UserRepository;
import com.homeease.homeease.repository.WorkerRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Controller
@RequestMapping("/dev")
public class DevController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/create-test-users")
    @ResponseBody
    public String createTestUsers() {
        StringBuilder sb = new StringBuilder();

        // Test user
        String userEmail = "test.user@example.com";
        if (userRepository.findByEmail(userEmail).isEmpty()) {
            User u = new User();
            u.setName("Test User");
            u.setEmail(userEmail);
            u.setPassword(passwordEncoder.encode("password"));
            u.setMobile("9999999999");
            u.setCity("City");
            u.setState("State");
            u.setPincode("123456");
            u.setRole(Role.USER);
            u.setEnabled(true);
            userRepository.save(u);
            sb.append("Created user: ").append(userEmail).append(" / password\n");
        } else {
            sb.append("User exists: ").append(userEmail).append("\n");
        }

        // Test worker
        String workerEmail = "test.worker@example.com";
        if (workerRepository.findByEmail(workerEmail).isEmpty()) {
            Worker w = new Worker();
            w.setName("Test Worker");
            w.setEmail(workerEmail);
            w.setPassword(passwordEncoder.encode("password"));
            w.setMobile("8888888888");
            w.setCity("City");
            w.setState("State");
            w.setPincode("123456");
            w.setCategory("Plumber");
            w.setRole(Role.WORKER);
            w.setEnabled(true);
            w.setApproved(true);
            workerRepository.save(w);
            sb.append("Created worker: ").append(workerEmail).append(" / password\n");
        } else {
            sb.append("Worker exists: ").append(workerEmail).append("\n");
        }

        return sb.toString();
    }

    @GetMapping("/create-admin")
    @ResponseBody
    public String createAdmin() {
        String adminEmail = "admin@example.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User a = new User();
            a.setName("Administrator");
            a.setEmail(adminEmail);
            a.setPassword(passwordEncoder.encode("adminpass"));
            a.setMobile("7777777777");
            a.setCity("City");
            a.setState("State");
            a.setPincode("000000");
            a.setRole(Role.ADMIN);
            a.setEnabled(true);
            userRepository.save(a);
            return "Created admin: " + adminEmail + " / adminpass";
        } else {
            return "Admin exists: " + adminEmail;
        }
    }

    @GetMapping("/user-info")
    @ResponseBody
    public String userInfo(String email) {
        if (email == null || email.isBlank()) return "provide ?email=...";
        var uOpt = userRepository.findByEmail(email);
        if (uOpt.isEmpty()) return "User not found: " + email;
        User u = uOpt.get();
        return "User:" + u.getEmail() + " enabled=" + u.isEnabled() + " role=" + u.getRole();
    }

    @GetMapping("/worker-info")
    @ResponseBody
    public String workerInfo(String email) {
        if (email == null || email.isBlank()) return "provide ?email=...";
        var wOpt = workerRepository.findByEmail(email);
        if (wOpt.isEmpty()) return "Worker not found: " + email;
        Worker w = wOpt.get();
        return "Worker:" + w.getEmail() + " enabled=" + w.isEnabled() + " approved=" + w.isApproved() + " role=" + w.getRole();
    }
}
