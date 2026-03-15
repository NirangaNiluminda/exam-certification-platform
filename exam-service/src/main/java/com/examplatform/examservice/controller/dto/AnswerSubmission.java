package com.examplatform.examservice.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record AnswerSubmission(
        @NotNull UUID questionId,
        @NotBlank @Pattern(regexp = "[ABCD]") String selectedOption
) {}
