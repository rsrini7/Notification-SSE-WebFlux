package com.example.notification.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityDebugFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityDebugFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            log.debug("Request to {} by user: {} with roles: {}", request.getRequestURI(), auth.getName(), auth.getAuthorities());
        } else {
            log.debug("Request to {} with no authentication", request.getRequestURI());
        }
        filterChain.doFilter(request, response);
    }
}