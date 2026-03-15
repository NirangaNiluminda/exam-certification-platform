package com.examplatform.enrollment.controller;

import com.examplatform.enrollment.controller.dto.EnrollRequest;
import com.examplatform.enrollment.controller.dto.EnrollmentResponse;
import com.examplatform.enrollment.domain.Enrollment;
import com.examplatform.enrollment.domain.EnrollmentRepository;
import com.examplatform.enrollment.domain.EnrollmentStatus;
import com.examplatform.enrollment.saga.EnrollmentSaga;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST entry point for student enrollments.
 *
 * POST /api/v1/enrollments          → initiate enrollment SAGA
 * GET  /api/v1/enrollments/me       → list my enrollments (by studentId header)
 * GET  /api/v1/enrollments/{id}     → get enrollment status
 */
@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentSaga enrollmentSaga;
    private final EnrollmentRepository enrollmentRepository;

    @PostMapping
    public ResponseEntity<EnrollmentResponse> enroll(
            @Valid @RequestBody EnrollRequest request,
            @RequestHeader("X-Student-Id")    String studentId,
            @RequestHeader("X-Student-Email") String studentEmail,
            @RequestHeader("X-Student-Name")  String studentName) {

        Enrollment enrollment = enrollmentSaga.initiate(
                UUID.fromString(studentId),
                request.examId(),
                studentEmail,
                studentName,
                request.examPrice()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EnrollmentResponse.from(enrollment));
    }

    @GetMapping("/me")
    public ResponseEntity<List<EnrollmentResponse>> myEnrollments(
            @RequestHeader("X-Student-Id") String studentId) {

        List<EnrollmentResponse> enrollments = enrollmentRepository
                .findByStudentId(UUID.fromString(studentId))
                .stream()
                .map(EnrollmentResponse::from)
                .toList();

        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentResponse> getEnrollment(@PathVariable UUID id) {
        return enrollmentRepository.findById(id)
                .map(e -> ResponseEntity.ok(EnrollmentResponse.from(e)))
                .orElse(ResponseEntity.notFound().build());
    }
}
