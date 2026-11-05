package com.homeease.homeease.security;

import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Worker;
import com.homeease.homeease.repository.UserRepository;
import com.homeease.homeease.repository.WorkerRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        boolean isUser = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
        boolean isWorker = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_WORKER"));

        // Check if Spring saved a previous URL before redirecting to login
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String savedUrl = savedRequest.getRedirectUrl();
            if (savedUrl != null
                    && ((isUser && savedUrl.contains("/user/"))
                            || (isWorker && savedUrl.contains("/worker/")))) {
                requestCache.removeRequest(request, response);
                redirectStrategy.sendRedirect(request, response, savedUrl);
                return;
            }
            // Clear mismatched saved request
            requestCache.removeRequest(request, response);
        }

        // Default role-based redirect
        String targetUrl = "/";
        String email = authentication.getName();

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();

            if ("ROLE_ADMIN".equals(role)) {
                targetUrl = "/admin/dashboard";
                break;

            } else if ("ROLE_USER".equals(role)) {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null && "0000000000".equals(user.getMobile())) {
                    targetUrl = "/user/complete-profile";
                } else {
                    targetUrl = "/user/dashboard";
                }
                break;

            } else if ("ROLE_WORKER".equals(role)) {
                Worker worker = workerRepository.findByEmail(email).orElse(null);
                if (worker != null && "0000000000".equals(worker.getMobile())) {
                    targetUrl = "/worker/complete-profile";
                } else if (worker != null && !worker.isApproved()) {
                    request.getSession().invalidate();
                    org.springframework.security.core.context.SecurityContextHolder.clearContext();
                    targetUrl = "/login?error=pending_approval";
                } else {
                    targetUrl = "/worker/dashboard";
                }
                break;
            }
        }

        redirectStrategy.sendRedirect(request, response, targetUrl);
    }
}