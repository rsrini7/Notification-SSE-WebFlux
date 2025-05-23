package com.example.notification.config;

import com.example.notification.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor == null) {
            log.warn("No StompHeaderAccessor found in message");
            return message;
        }

        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();
        
        log.debug("Processing STOMP {} command for session: {}", command, sessionId);

        // Only process CONNECT commands for authentication
        if (StompCommand.CONNECT.equals(command)) {
            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
            log.info("STOMP CONNECT received - Session: {}, Headers: {}", 
                    sessionId, accessor.toNativeHeaderMap());

            if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
                String jwt = authorizationHeader.substring(7).trim();
                log.debug("JWT token extracted, length: {}", jwt.length());
                
                try {
                    if (jwtTokenProvider.validateToken(jwt)) {
                        String userId = jwtTokenProvider.getUserIdFromJWT(jwt);
                        List<String> roles = jwtTokenProvider.getRolesFromJWT(jwt);
                        List<SimpleGrantedAuthority> authorities = roles.stream()
                                .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                                .collect(Collectors.toList());

                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                        
                        // Set the authentication in the accessor
                        accessor.setUser(authentication);
                        accessor.setLeaveMutable(true);
                        
                        log.info("User {} successfully authenticated for WebSocket session {}", 
                                userId, sessionId);
                    } else {
                        log.warn("Invalid JWT token in WebSocket CONNECT for session: {}", sessionId);
                        throw new AuthenticationCredentialsNotFoundException("Invalid JWT token");
                    }
                } catch (Exception e) {
                    log.error("Error authenticating WebSocket connection for session {}: {}", 
                            sessionId, e.getMessage(), e);
                    throw new AuthenticationCredentialsNotFoundException("Authentication failed", e);
                }
            } else {
                log.warn("Missing or invalid Authorization header in WebSocket CONNECT for session: {}", 
                        sessionId);
                throw new AuthenticationCredentialsNotFoundException("No Authorization header or invalid format");
            }
        }
        // Handle other STOMP commands if needed
        else if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
            // Log subscription and send commands for debugging
            log.debug("STOMP {} - Destination: {}, Session: {}", 
                    command, 
                    accessor.getDestination(), 
                    sessionId);
        }
        
        return message;
    }
}