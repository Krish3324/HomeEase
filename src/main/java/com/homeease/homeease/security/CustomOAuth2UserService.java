package com.homeease.homeease.security;

import com.homeease.homeease.model.Role;
import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.repository.UserRepository;
import com.homeease.homeease.repository.WorkerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // ── Read role from session ──
        String role = "USER";
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String sessionRole = (String) request.getSession()
                    .getAttribute(CustomAuthorizationRequestRepository.OAUTH2_ROLE_KEY);
            if (sessionRole != null) {
                role = sessionRole;
                request.getSession()
                        .removeAttribute(CustomAuthorizationRequestRepository.OAUTH2_ROLE_KEY);
            }
        } catch (Exception ignored) {
        }

        // ── Already exists as WORKER ──
        if (workerRepository.findByEmail(email).isPresent()) {
            return new DefaultOAuth2User(
                    List.of(new SimpleGrantedAuthority("ROLE_WORKER")),
                    oAuth2User.getAttributes(), "email");
        }

        // ── Already exists as USER ──
        if (userRepository.findByEmail(email).isPresent()) {
            User user = userRepository.findByEmail(email).get();
            return new DefaultOAuth2User(
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                    oAuth2User.getAttributes(), "email");
        }

        // ── New WORKER registration ──
        if ("WORKER".equalsIgnoreCase(role)) {
            Worker worker = new Worker();
            worker.setName(name);
            worker.setEmail(email);
            worker.setPassword(UUID.randomUUID().toString());
            worker.setMobile("0000000000");
            worker.setCity("Unknown");
            worker.setState("Unknown");
            worker.setPincode("000000");
            worker.setCategory("Unknown");
            worker.setIdImagePath("pending");
            worker.setRole(Role.WORKER);
            worker.setEnabled(false);        // blocked until admin approves
            worker.setEmailVer(1);           // Google = email already verified
            worker.setApproved(false);
            worker.setPublicUserId(generatePublicId("WRK"));
            workerRepository.save(worker);

            return new DefaultOAuth2User(
                    List.of(new SimpleGrantedAuthority("ROLE_WORKER")),
                    oAuth2User.getAttributes(), "email");

        } else {
            // ── New USER registration ──
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(UUID.randomUUID().toString());
            user.setMobile("0000000000");
            user.setCity("Unknown");
            user.setState("Unknown");
            user.setPincode("000000");
            user.setRole(Role.USER);
            user.setEnabled(true);
            user.setEmailVer(1);
            user.setPublicUserId(generatePublicId("USR"));
            userRepository.save(user);

            return new DefaultOAuth2User(
                    List.of(new SimpleGrantedAuthority("ROLE_USER")),
                    oAuth2User.getAttributes(), "email");
        }
    }

    private String generatePublicId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 12).toUpperCase();
    }
}
