package com.example.notification.controller;

import com.example.notification.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        var roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String token = jwtTokenProvider.generateToken(loginRequest.getUsername(), roles);

        return ResponseEntity.ok().body(
                new LoginResponse(token, loginRequest.getUsername(), roles)
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

    public static class LoginResponse {
        private String token;
        private String username;
        private java.util.List<String> roles;

        public LoginResponse(String token, String username, java.util.List<String> roles) {
            this.token = token;
            this.username = username;
            this.roles = roles;
        }

        public String getToken() { return token; }
        public String getUsername() { return username; }
        public java.util.List<String> getRoles() { return roles; }
    }
}
