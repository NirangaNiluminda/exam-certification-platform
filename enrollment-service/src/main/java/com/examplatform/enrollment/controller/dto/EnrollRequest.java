package com.examplatform.enrollment.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record EnrollRequest(
        @NotNull UUID examId,
        @Positive double examPrice
) {}
