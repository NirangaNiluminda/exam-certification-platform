package com.examplatform.auth.controller;

import com.examplatform.auth.controller.dto.LoginRequest;
import com.examplatform.auth.controller.dto.RegisterRequest;
import com.examplatform.auth.controller.dto.TokenResponse;
import com.examplatform.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for authentication.
 * All paths are PUBLIC — no JWT required.
 *
 * POST /api/v1/auth/register  → register a new student, returns JWT
 * POST /api/v1/auth/login     → login with email + password, returns JWT
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        TokenResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
