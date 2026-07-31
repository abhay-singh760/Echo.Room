package com.echoroom.config;

import com.echoroom.model.User;
import com.echoroom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/** Ensures a single administrator account exists so the admin dashboard is reachable on first run. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap-username}")
    private String adminUsername;

    @Value("${app.admin.bootstrap-password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }
        User admin = User.builder()
                .username(adminUsername)
                .email(adminUsername + "@echoroom.local")
                .passwordHash(passwordEncoder.encode(adminPassword))
                .roles(List.of("ROLE_USER", "ROLE_ADMIN"))
                .build();
        userRepository.save(admin);
        log.info("Bootstrapped admin account '{}'. Change the default password in production.", adminUsername);
    }
}
