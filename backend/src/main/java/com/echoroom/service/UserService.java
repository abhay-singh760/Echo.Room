package com.echoroom.service;

import com.echoroom.dto.AuthDtos.AuthResponse;
import com.echoroom.dto.AuthDtos.LoginRequest;
import com.echoroom.dto.AuthDtos.RegisterRequest;
import com.echoroom.exception.ApiExceptions.BadRequestException;
import com.echoroom.exception.ApiExceptions.ConflictException;
import com.echoroom.model.User;
import com.echoroom.repository.UserRepository;
import com.echoroom.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String[] AVATAR_COLORS = {
            "028090", "00A896", "02C39A", "F96167", "B85042", "6D2E46", "1E2761", "50808E"
    };

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new ConflictException("Username is already taken");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("Email is already registered");
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .roles(List.of("ROLE_USER"))
                .avatarColor(AVATAR_COLORS[Math.abs(req.getUsername().hashCode()) % AVATAR_COLORS.length])
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRoles());
        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid username or password");
        }

        user.setOnline(true);
        user.setLastSeen(Instant.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRoles());
        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();
    }

    public User requireByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found: " + username));
    }
}
