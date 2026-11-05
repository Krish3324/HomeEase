package com.homeease.homeease.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Role;
import com.homeease.homeease.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AdminUserSeeder {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner createAdminIfMissing() {
        return args -> {
            String adminEmail = System.getProperty("homeease.admin.email", "admin@example.com");
            String adminPassword = System.getProperty("homeease.admin.password", "admin123");

            userRepository.findByEmail(adminEmail).ifPresentOrElse(u -> {
                // already exists
            }, () -> {
                User admin = new User();
                admin.setName("Administrator");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setMobile("0000000000");
                admin.setCity("City");
                admin.setState("State");
                admin.setPincode("000000");
                admin.setRole(Role.ADMIN);
                admin.setEnabled(true);
                admin.setPublicUserId(java.util.UUID.randomUUID().toString());
                userRepository.save(admin);
                System.out.println("Created admin user: " + adminEmail + " (password from system property or default)");
            });
        };
    }
}
