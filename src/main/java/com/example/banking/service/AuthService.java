package com.example.banking.service;

import com.example.banking.dto.request.LoginRequest;
import com.example.banking.dto.request.RegisterRequest;
import com.example.banking.dto.response.AuthResponse;
import com.example.banking.entity.Customer;
import com.example.banking.entity.Role;
import com.example.banking.entity.User;
import com.example.banking.exception.BusinessException;
import com.example.banking.repository.CustomerRepository;
import com.example.banking.repository.RoleRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new BusinessException("Username already exists");
        if (customerRepository.existsByNationalId(req.getNationalId()))
            throw new BusinessException("National ID already registered");

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
            .orElseThrow(() -> new BusinessException("Default role not configured"));

        User user = User.builder()
            .username(req.getUsername())
            .email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword()))
            .enabled(true)
            .roles(Set.of(customerRole))
            .build();
        userRepository.save(user);

        Customer customer = Customer.builder()
            .customerNumber(generateCustomerNumber())
            .firstName(req.getFirstName())
            .lastName(req.getLastName())
            .email(req.getEmail())
            .phone(req.getPhone())
            .dateOfBirth(req.getDateOfBirth())
            .nationalId(req.getNationalId())
            .address(req.getAddress())
            .user(user)
            .build();
        customerRepository.save(customer);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails, Map.of("role", "ROLE_CUSTOMER"));
        return new AuthResponse(token, user.getUsername(), "ROLE_CUSTOMER");
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        User user = userRepository.findByUsername(req.getUsername()).orElseThrow();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String role = user.getRoles().iterator().next().getName();
        String token = jwtService.generateToken(userDetails, Map.of("role", role));
        return new AuthResponse(token, user.getUsername(), role);
    }

    private String generateCustomerNumber() {
        long count = customerRepository.count() + 1;
        return String.format("CIF-%06d", count);
    }
}