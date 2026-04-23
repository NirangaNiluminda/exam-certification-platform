package com.examplatform.examservice.controller.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ExamRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @NotNull @Min(1) @Max(300) Integer durationMinutes,
        @NotNull @Min(1) @Max(100) Integer passingScore,
        @NotNull @Min(1) Integer totalMarks,
        @NotNull @DecimalMin("0.00") BigDecimal price
) {}
