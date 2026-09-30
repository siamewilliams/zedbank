package com.example.banking.config;

import com.example.banking.entity.Role;
import com.example.banking.entity.User;
import com.example.banking.repository.RoleRepository;
import com.example.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seed("admin", "admin@bank.local", "Admin@123", "ROLE_ADMIN");
        seed("teller", "teller@bank.local", "Teller@123", "ROLE_TELLER");
    }

    private void seed(String username, String email, String password, String roleName) {
        if (userRepository.existsByUsername(username)) return;
        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalStateException(roleName + " not seeded in DB"));
        userRepository.save(User.builder()
            .username(username)
            .email(email)
            .password(passwordEncoder.encode(password))
            .enabled(true)
            .roles(Set.of(role))
            .build());
        log.info("Seeded user: {} / {}", username, password);
    }
}