package com.homeease.homeease.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.repository.UserRepository;
import com.homeease.homeease.repository.WorkerRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOpt = userRepository.findByEmail(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            log.debug("Found user record for {} enabled={} emailVer={} role={} passwordHash={}", 
                user.getEmail(), user.isEnabled(), user.getEmailVer(), user.getRole(), user.getPassword());
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            
            // User must be enabled by admin to login (email verification is separate)
            if (!user.isEnabled()) {
                throw new org.springframework.security.authentication.DisabledException(
                    user.getEmailVer() == 1 ? "admin_disabled" : "email_not_verified");
            }
            
            return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPassword(), true, true, true, true, authorities);
        }

        Optional<Worker> workerOpt = workerRepository.findByEmail(username);
        if (workerOpt.isPresent()) {
            Worker worker = workerOpt.get();
            log.debug("Found worker record for {} enabled={} approved={} emailVer={} role={} passwordHash={}", 
                worker.getEmail(), worker.isEnabled(), worker.isApproved(), worker.getEmailVer(), worker.getRole(), worker.getPassword());
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + worker.getRole().name()));
            
            // Worker must be enabled by admin to login (email verification is separate)
            if (!worker.isEnabled()) {
                throw new org.springframework.security.authentication.DisabledException(
                    worker.getEmailVer() == 1 ? "admin_disabled" : "email_not_verified");
            }
            
            return new org.springframework.security.core.userdetails.User(
                worker.getEmail(), worker.getPassword(), true, true, true, true, authorities);
        }

        log.debug("No user/worker found with email: {}", username);
        throw new UsernameNotFoundException("User not found with email: " + username);
    }
}
