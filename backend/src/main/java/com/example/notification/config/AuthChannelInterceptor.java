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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final WebSocketSessionRegistry sessionRegistry;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            log.warn("No StompHeaderAccessor found in message");
            return message;
        }

        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();

        //SKIP LOGGING FOR HEARTBEATS
        if (command == null) {
            // This is likely a heartbeat frame or a non-STOMP message after WebSocket session is established.
            // You can use TRACE level logging if you want to see these, e.g.:
            // log.trace("Received heartbeat or non-STOMP message on session: {}", sessionId);
            return message; // Let it pass through without further AuthChannelInterceptor processing or logging as a "command"
        }

        log.debug("Processing STOMP {} command for session: {}", command, sessionId);

        try {
            // Handle different STOMP commands
            if (StompCommand.CONNECT.equals(command)) {
                handleConnect(accessor, sessionId);
            } else if (StompCommand.CONNECTED.equals(command)) {
                log.info("STOMP CONNECTED - Session: {}", sessionId);
                // Register the session after successful connection
                if (accessor.getUser() != null && accessor.getUser() instanceof Authentication) {
                    sessionRegistry.registerSession(sessionId, (Authentication) accessor.getUser());
                }
            } else if (StompCommand.DISCONNECT.equals(command)) {
                sessionRegistry.removeSession(sessionId);
                log.info("STOMP DISCONNECT - Session: {}", sessionId);
            } else if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
                handleSubscriptionOrSend(accessor, sessionId, command);
            }

            // For all commands except CONNECT, ensure there's an authenticated user
            if (command != null && !StompCommand.CONNECT.equals(command)) {
                Object principal = accessor.getUser();
                if (principal == null || 
                    !(principal instanceof Authentication) || 
                    !((Authentication) principal).isAuthenticated()) {
                    log.warn("Unauthorized access attempt for command: {}, Session: {}", command, sessionId);
                    throw new AuthenticationCredentialsNotFoundException("Not authenticated");
                }
            }

        } catch (AuthenticationCredentialsNotFoundException e) {
            log.warn("Authentication failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error processing STOMP command {}: {}", command, e.getMessage(), e);
            throw new AuthenticationCredentialsNotFoundException("Authentication error", e);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor, String sessionId) {
        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
        String userIdHeader = accessor.getFirstNativeHeader("user-id");

        log.info("STOMP CONNECT received - Session: {}, Headers: {}",
                sessionId, accessor.toNativeHeaderMap());

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7).trim();
            log.debug("JWT token extracted, length: {}", jwt.length());
            
            try {
                if (jwtTokenProvider.validateToken(jwt)) {
                    String userId = jwtTokenProvider.getUserIdFromJWT(jwt);

                    // Validate userId from token matches the one in headers if provided
                    if (userIdHeader != null && !userId.equals(userIdHeader)) {
                        log.warn("User ID mismatch - token: {}, header: {}", userId, userIdHeader);
                        throw new AuthenticationCredentialsNotFoundException("User ID mismatch");
                    }
                    
                    List<String> roles = jwtTokenProvider.getRolesFromJWT(jwt);
                    List<GrantedAuthority> authorities = roles.stream()
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
            } catch (AuthenticationCredentialsNotFoundException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error authenticating WebSocket connection for session {}: {}",
                        sessionId, e.getMessage(), e);
                throw new AuthenticationCredentialsNotFoundException("Authentication failed: " + e.getMessage(), e);
            }
        } else {
            log.warn("Missing or invalid Authorization header in WebSocket CONNECT for session: {}",
                    sessionId);
            throw new AuthenticationCredentialsNotFoundException("No valid Authorization header with Bearer token");
        }
    }

    // Session management is now handled by WebSocketSessionRegistry
    // The DISCONNECT command handler calls sessionRegistry.removeSession() directly

    private void handleSubscriptionOrSend(StompHeaderAccessor accessor, String sessionId, StompCommand command) {
        Object principal = accessor.getUser();
        String destination = accessor.getDestination();
        Authentication auth = null;

        // Check if principal is already an authenticated Authentication object
        if (principal instanceof Authentication && ((Authentication) principal).isAuthenticated()) {
            auth = (Authentication) principal;
        } 
        // Otherwise try to get from session registry
        else {
            auth = sessionRegistry.getAuthentication(sessionId);
            if (auth != null) {
                accessor.setUser(auth);
            } else {
                log.warn("No authentication found for session {} in {}", sessionId, command);
                throw new AuthenticationCredentialsNotFoundException("Not authenticated");
            }
        }

        log.debug("STOMP {} - Destination: {}, User: {}, Session: {}",
                command,
                destination,
                auth.getName(),
                sessionId);
    }
}