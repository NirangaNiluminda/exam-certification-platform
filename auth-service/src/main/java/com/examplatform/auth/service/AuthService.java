package com.examplatform.auth.service;

import com.examplatform.auth.controller.dto.LoginRequest;
import com.examplatform.auth.controller.dto.RegisterRequest;
import com.examplatform.auth.controller.dto.TokenResponse;
import com.examplatform.auth.domain.Role;
import com.examplatform.auth.domain.User;
import com.examplatform.auth.domain.UserRepository;
import com.examplatform.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(Role.STUDENT)  // all self-registrations are STUDENT by default
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("New student registered: {}", user.getEmail());

        String token = jwtService.generateToken(user);
        return new TokenResponse(token, user.getId().toString(),
                user.getEmail(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        log.info("User logged in: {}", user.getEmail());
        return new TokenResponse(token, user.getId().toString(),
                user.getEmail(), user.getRole().name());
    }
}
