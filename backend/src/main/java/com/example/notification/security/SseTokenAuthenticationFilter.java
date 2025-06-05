package com.example.notification.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SseTokenAuthenticationFilter implements Filter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String requestPath = request.getServletPath();

        if ("/api/notifications/events".equals(requestPath)) {
            log.debug("SSE_AUTH_FILTER: Processing request for path: {}", requestPath);
            String jwt = request.getParameter("token");

            if (StringUtils.hasText(jwt)) {
                log.debug("SSE_AUTH_FILTER: Token found in query param for {}. Validating token: {}", requestPath, jwt);
                boolean isValidToken = jwtTokenProvider.validateToken(jwt);
                log.debug("SSE_AUTH_FILTER: Token validation result for {}: {}", requestPath, isValidToken);

                if (isValidToken) {
                    String userId = jwtTokenProvider.getUserIdFromJWT(jwt);
                    List<String> roles = jwtTokenProvider.getRolesFromJWT(jwt);

                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("SSE_AUTH_FILTER: Authentication successfully set in SecurityContext for user: {}, roles: {}.", userId, roles);
                } else {
                    log.warn("SSE_AUTH_FILTER: Invalid JWT token received for path {}. Token: {}", requestPath, jwt);
                    // If token is invalid, we don't set auth, AnonymousAuth will kick in later if not handled otherwise.
                }
            } else {
                log.debug("SSE_AUTH_FILTER: No JWT token found in query param for path {}.", requestPath);
            }
        }

        chain.doFilter(req, res);
    }
}
