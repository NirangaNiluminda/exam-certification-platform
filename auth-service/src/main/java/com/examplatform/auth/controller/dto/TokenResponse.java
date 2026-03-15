package com.examplatform.auth.controller.dto;

public record TokenResponse(
        String accessToken,
        String userId,
        String email,
        String role
) {}
