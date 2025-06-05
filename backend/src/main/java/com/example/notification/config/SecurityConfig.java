package com.example.notification.config;

import java.util.Arrays;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.notification.security.CustomAuthenticationProvider;
import com.example.notification.security.JwtAuthenticationFilter;
import com.example.notification.security.SseTokenAuthenticationFilter; // Import new filter

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private SecurityDebugFilter securityDebugFilter;

    @Autowired // Added for the new filter
    private SseTokenAuthenticationFilter sseTokenAuthenticationFilter;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Configure CORS
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:3001"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "user-id"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        http
            .cors(cors -> cors.configurationSource(source))
            .csrf(csrf -> csrf
                .disable() // Disable CSRF for stateless authentication
            )
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints
                .requestMatchers(
                    "/h2-console/**",
                    "/topic/**",
                    "/queue/**",
                    "/user/queue/**",
                    "/app/**",
                    "/api/auth/login",
                    "/api/auth/register",
                    "/favicon.ico",
                    "/logo192.png",
                    "/static/**",
                    "/assets/**",
                    "/manifest.json",
                    "/api/test/**" // Allow SSE connections <-- Line removed, comma managed
                ).permitAll()
                // Admin endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // All other requests need to be authenticated
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .headers(headers -> headers.frameOptions().sameOrigin())
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint((request, response, authException) -> {
                    if (!response.isCommitted()) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (!response.isCommitted()) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                    }
                })
            )
            // Add this line to prevent recursive authentication attempts
            .formLogin(formLogin -> formLogin.disable())
            // Add filters before UsernamePasswordAuthenticationFilter.
            // The filter added in the LAST call to addFilterBefore for a given anchor comes EARLIEST.
            // So, to get order: SseTokenAuth -> JwtAuth -> SecurityDebug -> UsernamePasswordAuth:
            .addFilterBefore(securityDebugFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(sseTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            // No addFilterAfter for securityDebugFilter is needed now as it's ordered here.

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Lower cost for faster hashing in development (do not use in production)
        return new BCryptPasswordEncoder(4);
    }
    
    @Bean
    public UserDetailsService userDetailsService(DataSource dataSource) {
        JdbcUserDetailsManager userDetailsManager = new JdbcUserDetailsManager(dataSource);
        userDetailsManager.setUsersByUsernameQuery(
            "SELECT username, password, enabled FROM users WHERE username = ?");
        userDetailsManager.setAuthoritiesByUsernameQuery(
            "SELECT username, authority FROM authorities WHERE username = ?");
        return userDetailsManager;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, 
                                                      CustomAuthenticationProvider authProvider) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
            .authenticationProvider(authProvider)
            .build();
    }


}
