package com.ridesharing.project.config.websocket;

 
import lombok.RequiredArgsConstructor;

import java.util.Collection;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.prad.starter.jwt.JwtUtil;
import com.ridesharing.project.service.security.CustomUserDetailsService;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

        private final JwtUtil jwtUtils;
        private final CustomUserDetailsService userDetailsService;
        
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {

                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) return message;

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        throw new IllegalArgumentException("Missing or invalid Authorization header");
                    }

                    String token = authHeader.substring(7);

                    if (!jwtUtils.validateJwtToken(token)) {
                        throw new IllegalArgumentException("Invalid JWT token");
                    }

                    String username = jwtUtils.getUserNameFromJwtToken(token);
                    //String role = jwtUtils.extractRole(token); // if you store role in JWT
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // get authorities from userDetails
                    Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    authorities
                            );

                    accessor.setUser(authentication); // ← the key call
                }

                return message;
            }
}
