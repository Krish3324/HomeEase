package com.homeease.homeease.security;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private static final org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {

        String username = request.getParameter("username");
        log.warn("Auth failure user='{}' exception='{}' message='{}'",
                username, exception.getClass().getSimpleName(), exception.getMessage());

        String reason;
        if (exception instanceof DisabledException) {
            String message = exception.getMessage();
            if ("admin_disabled".equals(message)) {
                reason = "admin_disabled";
            } else if ("email_not_verified".equals(message)) {
                reason = "disabled"; // Keep existing "disabled" for email verification
            } else {
                reason = "disabled";
            }
        } else if (exception instanceof LockedException) {
            reason = "locked";
        } else {
            reason = "bad_credentials";
        }

        redirectStrategy.sendRedirect(request, response, "/login?error=" + reason);
    }
}
