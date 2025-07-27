package com.example.notification.controller;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    private final Cache pendingNotificationsCache;

    public AuthController(AuthenticationManager authenticationManager,
                         JwtTokenProvider jwtTokenProvider,
                         PasswordEncoder passwordEncoder,
                         JdbcTemplate jdbcTemplate,
                         CacheManager cacheManager) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.pendingNotificationsCache = cacheManager.getCache("pendingNotificationsCache");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            // Check if username already exists
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = ?",
                    Integer.class,
                    registerRequest.getUsername());
                    
            if (count != null && count > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponse("Username already exists"));
            }
            
            // Encode password with BCrypt
            String encodedPassword = passwordEncoder.encode(registerRequest.getPassword());
            
            // Insert new user with email field
            jdbcTemplate.update(
                    "INSERT INTO users (username, password, enabled) VALUES (?, ?, ?)",
                    registerRequest.getUsername(), 
                    encodedPassword,
                    true);
                    
            // Add default USER role
            jdbcTemplate.update(
                    "INSERT INTO authorities (username, authority) VALUES (?, ?)",
                    registerRequest.getUsername(), 
                    "ROLE_USER");
                    
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new RegisterResponse("User registered successfully"));
        } catch (Exception e) {
            log.error("Error during registration: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Registration failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            log.info("Authentication successful for user: {}", loginRequest.getUsername());

            var roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            log.info("User '{}' roles: {}", loginRequest.getUsername(), roles);

            String token = jwtTokenProvider.generateToken(loginRequest.getUsername(), roles);

            List<NotificationResponse> pending = pendingNotificationsCache.get(loginRequest.getUsername(), List.class);
            if (pending != null && !pending.isEmpty()) {
                log.info("Found {} pending notifications for user {}", pending.size(), loginRequest.getUsername());
                // Remove them from cache after retrieval
                pendingNotificationsCache.evict(loginRequest.getUsername());
            }

            return ResponseEntity.ok().body(
                    new LoginResponse(token, loginRequest.getUsername(), roles, pending)
            );
        } catch (Exception ex) {
            log.error("Authentication failed for user: {}: {}", loginRequest.getUsername(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid username or password"));
        }
    }
    
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            
            if (jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUserIdFromJWT(token);
                List<String> roles = jwtTokenProvider.getRolesFromJWT(token);
                
                return ResponseEntity.ok().body(
                    new LoginResponse(token, username, roles, pendingNotificationsCache.get(username, List.class))
                );
            }
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            new ErrorResponse("Invalid or expired token")
        );
    }

    public static class LoginRequest {
        private String username;
        private String password;
        private String role; // optional, ignored here

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class RegisterRequest {
        private String username;
        private String password;
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class RegisterResponse {
        private String message;
        
        public RegisterResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() { return message; }
    }
    
    public static class ErrorResponse {
        private String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
        
        public String getError() { return error; }
    }
    
    public static class LoginResponse {
        private String token;
        private String username;
        private java.util.List<String> roles;
        private java.util.List<NotificationResponse> pendingNotifications;

        public LoginResponse(String token, String username, java.util.List<String> roles, java.util.List<NotificationResponse> pendingNotifications) {
            this.token = token;
            this.username = username;
            this.roles = roles;
            this.pendingNotifications = pendingNotifications;
        }

        public String getToken() { return token; }
        public String getUsername() { return username; }
        public java.util.List<String> getRoles() { return roles; }
        public java.util.List<NotificationResponse> getPendingNotifications() { return pendingNotifications; }
    }
}
