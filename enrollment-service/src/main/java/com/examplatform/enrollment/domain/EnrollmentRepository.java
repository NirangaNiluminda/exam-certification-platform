package com.examplatform.enrollment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    List<Enrollment> findByStudentId(UUID studentId);
    Optional<Enrollment> findByStudentIdAndExamId(UUID studentId, UUID examId);
    boolean existsByStudentIdAndExamIdAndStatusNot(UUID studentId, UUID examId, EnrollmentStatus status);
}
