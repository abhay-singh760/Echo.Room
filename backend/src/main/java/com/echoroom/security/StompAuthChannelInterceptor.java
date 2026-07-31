package com.echoroom.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Authenticates the STOMP CONNECT frame using the JWT sent in the
 * "Authorization" native header. The resulting Principal (server-verified
 * username) is what every subsequent @MessageMapping handler trusts --
 * never a client-supplied username field in the payload.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = firstNativeHeader(accessor, "Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.isTokenValid(token)) {
                    String username = jwtUtil.extractUsername(token);
                    List<String> roles = jwtUtil.extractRoles(token);
                    var authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                    Principal principal = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    accessor.setUser(principal);
                } else {
                    throw new IllegalArgumentException("Invalid or expired token");
                }
            } else {
                throw new IllegalArgumentException("Missing Authorization header on STOMP CONNECT");
            }
        }
        return message;
    }

    private String firstNativeHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}
