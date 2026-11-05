package com.homeease.homeease.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

@Configuration
public class SecurityConfig {

        @Autowired
        private CustomUserDetailsService userDetailsService;

        @Autowired
        private BCryptPasswordEncoder passwordEncoder;

        @Autowired
        private CustomOAuth2UserService customOAuth2UserService;

        @Autowired
        private CustomAuthorizationRequestRepository authorizationRequestRepository;

        @Autowired
        private RoleBasedAuthenticationSuccessHandler successHandler;

        @Autowired
        private CustomAuthenticationFailureHandler failureHandler;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

                DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
                provider.setUserDetailsService(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder);
                http.authenticationProvider(provider);

                // ── Enable request cache so Spring saves the original URL
                // before redirecting to login (e.g. /user/hire/23)
                // After login, RoleBasedAuthenticationSuccessHandler reads it
                // and redirects back to the original page
                HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
                requestCache.setMatchingRequestParameterName(null);
                http.requestCache(cache -> cache.requestCache(requestCache));

                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/", "/login", "/register-user", "/register-worker",
                                                                "/explore_service", "/services", "/auth/**",
                                                                "/worker-profile/**",
                                                                "/css/**", "/js/**", "/images/**")
                                                .permitAll()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/worker/go-online").hasRole("WORKER")
                                                .requestMatchers("/worker/go-offline").hasRole("WORKER")
                                                .requestMatchers("/user/complete-profile").authenticated()
                                                .requestMatchers("/user/**").authenticated()
                                                .requestMatchers("/worker/complete-profile").authenticated()
                                                .requestMatchers("/worker/**").hasRole("WORKER")
                                                .requestMatchers("/api/payment/**").authenticated()
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .accessDeniedPage("/access-denied"))
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .successHandler(successHandler)
                                                .failureHandler(failureHandler)
                                                .permitAll())
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login")
                                                .authorizationEndpoint(auth -> auth
                                                                .authorizationRequestRepository(
                                                                                authorizationRequestRepository))
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(successHandler)
                                                .failureUrl("/login?error=oauth_failed"))
                                .rememberMe(remember -> remember
                                                .key("homeeaseSecretKey123")
                                                .tokenValiditySeconds(86400 * 30) // 30 days
                                                .userDetailsService(userDetailsService)
                                                .rememberMeParameter("remember-me"))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll());

                http.headers(headers -> headers
                                .cacheControl(cache -> cache.disable()));

                http.headers(headers -> headers
                                .frameOptions(frame -> frame.sameOrigin()));

                return http.build();
        }
}