package com.examplatform.enrollment.controller.dto;

import com.examplatform.enrollment.domain.Enrollment;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(
        UUID   id,
        UUID   studentId,
        UUID   examId,
        String status,
        Instant enrolledAt
) {
    public static EnrollmentResponse from(Enrollment e) {
        return new EnrollmentResponse(
                e.getId(), e.getStudentId(), e.getExamId(),
                e.getStatus().name(), e.getEnrolledAt());
    }
}
