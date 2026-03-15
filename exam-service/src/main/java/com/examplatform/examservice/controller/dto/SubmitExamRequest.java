package com.examplatform.examservice.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitExamRequest(
        @NotEmpty @Valid List<AnswerSubmission> answers
) {}
