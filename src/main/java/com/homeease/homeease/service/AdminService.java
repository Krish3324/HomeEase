package com.homeease.homeease.service;

import com.homeease.homeease.model.*;
import com.homeease.homeease.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Transactional
    public void deleteWorker(Long id) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        String email = worker.getEmail();
        System.out.println("DEBUG: Starting JDBC delete for worker: " + email);

        // 1. Clear Hibernate session
        entityManager.flush();
        entityManager.clear();

        // Get list of actual tables (lowercase for safe comparison)
        List<String> existingTables = jdbcTemplate.queryForList("SHOW TABLES", String.class)
                .stream().map(String::toLowerCase).collect(java.util.stream.Collectors.toList());

        try {
            // 2. Disable FK checks
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");

            // 3. Delete from all known plural and singular tables IF they exist
            String[] targetTables = {
                "work_proofs", "work_proof", "messages", "message", "notifications", "notification", 
                "ratings", "rating", "payments", "payment", "reports", "report", 
                "transactions", "transaction", "bank_details", "bank_detail", "bookings", "booking"
            };

            for (String table : targetTables) {
                if (existingTables.contains(table.toLowerCase())) {
                    // Delete by worker_id
                    try { jdbcTemplate.update("DELETE FROM " + table + " WHERE worker_id = ?", id); } catch (Exception e) {}
                    
                    // Delete sub-children linked via booking_id
                    if (existingTables.contains("bookings")) {
                        try { jdbcTemplate.update("DELETE FROM " + table + " WHERE booking_id IN (SELECT id FROM bookings WHERE worker_id = ?)", id); } catch (Exception e) {}
                    }
                    if (existingTables.contains("booking")) {
                        try { jdbcTemplate.update("DELETE FROM " + table + " WHERE booking_id IN (SELECT id FROM booking WHERE worker_id = ?)", id); } catch (Exception e) {}
                    }
                }
            }

            // Reports by email
            if (existingTables.contains("reports")) {
                jdbcTemplate.update("DELETE FROM reports WHERE target_email = ? OR sender_email = ?", email, email);
            }
            if (existingTables.contains("report")) {
                jdbcTemplate.update("DELETE FROM report WHERE target_email = ? OR sender_email = ?", email, email);
            }

            // 4. Final worker deletion
            jdbcTemplate.update("DELETE FROM workers WHERE id = ?", id);
            
            System.out.println("DEBUG: Worker deleted successfully: " + email);
        } finally {
            // 5. Always re-enable FK checks
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Cannot delete admin user");
        }

        String email = user.getEmail();
        System.out.println("DEBUG: Starting JDBC delete for user: " + email);

        // 1. Clear Hibernate session
        entityManager.flush();
        entityManager.clear();

        // Get list of actual tables
        List<String> existingTables = jdbcTemplate.queryForList("SHOW TABLES", String.class)
                .stream().map(String::toLowerCase).collect(java.util.stream.Collectors.toList());

        try {
            // 2. Disable FK checks
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");

            // 3. Delete from all tables IF they exist
            String[] targetTables = {
                "work_proofs", "work_proof", "messages", "message", "notifications", "notification", 
                "ratings", "rating", "payments", "payment", "reports", "report", 
                "transactions", "transaction", "bookings", "booking"
            };

            for (String table : targetTables) {
                if (existingTables.contains(table.toLowerCase())) {
                    try { jdbcTemplate.update("DELETE FROM " + table + " WHERE user_id = ?", id); } catch (Exception e) {}
                    try { jdbcTemplate.update("DELETE FROM " + table + " WHERE sender_id = ?", id); } catch (Exception e) {}

                    if (existingTables.contains("bookings")) {
                        try { jdbcTemplate.update("DELETE FROM " + table + " WHERE booking_id IN (SELECT id FROM bookings WHERE user_id = ?)", id); } catch (Exception e) {}
                    }
                    if (existingTables.contains("booking")) {
                        try { jdbcTemplate.update("DELETE FROM " + table + " WHERE booking_id IN (SELECT id FROM booking WHERE user_id = ?)", id); } catch (Exception e) {}
                    }
                }
            }

            // Reports by email
            if (existingTables.contains("reports")) {
                jdbcTemplate.update("DELETE FROM reports WHERE sender_email = ? OR target_email = ?", email, email);
            }
            if (existingTables.contains("report")) {
                jdbcTemplate.update("DELETE FROM report WHERE sender_email = ? OR target_email = ?", email, email);
            }

            // 4. Final user deletion
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
            
            System.out.println("DEBUG: User deleted successfully: " + email);
        } finally {
            // 5. Re-enable FK checks
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }
}
