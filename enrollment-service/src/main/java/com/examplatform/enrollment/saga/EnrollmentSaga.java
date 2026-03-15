package com.examplatform.enrollment.saga;

import com.examplatform.enrollment.domain.*;
import com.examplatform.outbox.OutboxEventDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * SAGA Orchestrator for the enrollment flow.
 *
 * State machine:
 *   STARTED → [payment-service] → PAYMENT_COMPLETED
 *           → [exam-service]    → EXAM_SCHEDULED
 *           → [student takes exam]
 *           → [grading-service] → GRADED
 *           → [certificate-service] → COMPLETED
 *
 * On failure at any step: compensation events are published.
 *
 * Lecture reference: Cloud Application Architecture Example and Demo.pdf
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentSaga {

    private final EnrollmentRepository enrollmentRepository;
    private final SagaStateRepository  sagaStateRepository;
    private final OutboxEventDispatcher outbox;

    // ── Step 1: Initiate SAGA ────────────────────────────────────

    @Transactional
    public Enrollment initiate(UUID studentId, UUID examId, String studentEmail,
                               String studentName, double examPrice) {
        // Create enrollment
        Enrollment enrollment = Enrollment.builder()
                .studentId(studentId)
                .examId(examId)
                .status(EnrollmentStatus.PENDING)
                .build();
        enrollmentRepository.save(enrollment);

        // Create saga state
        SagaState saga = SagaState.builder()
                .enrollmentId(enrollment.getId())
                .sagaStatus(SagaStatus.STARTED)
                .currentStep("PAYMENT")
                .build();
        sagaStateRepository.save(saga);

        // Publish ENROLLMENT_INITIATED → triggers payment-service
        outbox.dispatch(
                enrollment.getId(),
                "Enrollment",
                "ENROLLMENT_INITIATED",
                "payment.inbox.events",
                Map.of(
                        "enrollmentId", enrollment.getId().toString(),
                        "studentId",    studentId.toString(),
                        "examId",       examId.toString(),
                        "studentEmail", studentEmail,
                        "studentName",  studentName,
                        "amount",       examPrice
                )
        );

        log.info("SAGA initiated for enrollment={} student={}", enrollment.getId(), studentId);
        return enrollment;
    }

    // ── Step 2: Payment completed → schedule exam slot ───────────

    @Transactional
    public void onPaymentCompleted(UUID enrollmentId, double amountPaid) {
        Enrollment enrollment = getEnrollment(enrollmentId);
        SagaState saga = getSaga(enrollmentId);

        enrollment.setStatus(EnrollmentStatus.PAYMENT_COMPLETED);
        saga.setSagaStatus(SagaStatus.PAYMENT_COMPLETED);
        saga.setCurrentStep("EXAM_SLOT");

        enrollmentRepository.save(enrollment);
        sagaStateRepository.save(saga);

        // Trigger exam slot scheduling
        outbox.dispatch(
                enrollmentId,
                "Enrollment",
                "SCHEDULE_EXAM_SLOT",
                "exam.inbox.events",
                Map.of(
                        "enrollmentId", enrollmentId.toString(),
                        "examId",       enrollment.getExamId().toString(),
                        "studentId",    enrollment.getStudentId().toString()
                )
        );

        log.info("SAGA step PAYMENT_COMPLETED for enrollment={}", enrollmentId);
    }

    // ── Step 3: Exam slot scheduled ──────────────────────────────

    @Transactional
    public void onExamSlotScheduled(UUID enrollmentId, String scheduledAt) {
        Enrollment enrollment = getEnrollment(enrollmentId);
        SagaState saga = getSaga(enrollmentId);

        enrollment.setStatus(EnrollmentStatus.EXAM_SCHEDULED);
        saga.setSagaStatus(SagaStatus.EXAM_SCHEDULED);
        saga.setCurrentStep("AWAITING_EXAM");

        enrollmentRepository.save(enrollment);
        sagaStateRepository.save(saga);

        // Notify student (async, fire and forget)
        outbox.dispatch(
                enrollmentId,
                "Enrollment",
                "SEND_NOTIFICATION",
                "notification.events",
                Map.of(
                        "type",         "EXAM_SCHEDULED",
                        "enrollmentId", enrollmentId.toString(),
                        "studentId",    enrollment.getStudentId().toString(),
                        "scheduledAt",  scheduledAt
                )
        );

        log.info("SAGA step EXAM_SCHEDULED for enrollment={} at={}", enrollmentId, scheduledAt);
    }

    // ── Step 4: Exam submitted → trigger grading ─────────────────

    @Transactional
    public void onExamSubmitted(UUID enrollmentId, UUID slotId) {
        Enrollment enrollment = getEnrollment(enrollmentId);
        SagaState saga = getSaga(enrollmentId);

        enrollment.setStatus(EnrollmentStatus.EXAM_SUBMITTED);
        saga.setSagaStatus(SagaStatus.EXAM_SUBMITTED);
        saga.setCurrentStep("GRADING");

        enrollmentRepository.save(enrollment);
        sagaStateRepository.save(saga);

        outbox.dispatch(
                enrollmentId,
                "Enrollment",
                "GRADE_EXAM",
                "grading.inbox.events",
                Map.of(
                        "enrollmentId", enrollmentId.toString(),
                        "slotId",       slotId.toString(),
                        "examId",       enrollment.getExamId().toString(),
                        "studentId",    enrollment.getStudentId().toString()
                )
        );

        log.info("SAGA step EXAM_SUBMITTED for enrollment={}", enrollmentId);
    }

    // ── Step 5: Graded → if PASS issue cert, if FAIL end saga ────

    @Transactional
    public void onExamGraded(UUID enrollmentId, boolean passed,
                              double percentage, String studentName, String examTitle) {
        Enrollment enrollment = getEnrollment(enrollmentId);
        SagaState saga = getSaga(enrollmentId);

        saga.setSagaStatus(SagaStatus.GRADED);
        saga.setCurrentStep(passed ? "CERTIFICATE" : "COMPLETE_FAILED");
        enrollment.setStatus(EnrollmentStatus.GRADED);

        enrollmentRepository.save(enrollment);
        sagaStateRepository.save(saga);

        if (passed) {
            outbox.dispatch(
                    enrollmentId,
                    "Enrollment",
                    "GENERATE_CERTIFICATE",
                    "certificate.inbox.events",
                    Map.of(
                            "enrollmentId", enrollmentId.toString(),
                            "studentId",    enrollment.getStudentId().toString(),
                            "examId",       enrollment.getExamId().toString(),
                            "studentName",  studentName,
                            "examTitle",    examTitle,
                            "score",        percentage
                    )
            );
            log.info("SAGA PASSED → certificate triggered for enrollment={}", enrollmentId);
        } else {
            completeFailed(enrollment, saga, "Exam not passed (score: " + percentage + "%)");
        }
    }

    // ── Step 6: Certificate issued → SAGA COMPLETED ──────────────

    @Transactional
    public void onCertificateIssued(UUID enrollmentId, String certificateUrl, String verifyCode) {
        Enrollment enrollment = getEnrollment(enrollmentId);
        SagaState saga = getSaga(enrollmentId);

        enrollment.setStatus(EnrollmentStatus.CERTIFIED);
        saga.setSagaStatus(SagaStatus.COMPLETED);
        saga.setCurrentStep(null);

        enrollmentRepository.save(enrollment);
        sagaStateRepository.save(saga);

        outbox.dispatch(
                enrollmentId,
                "Enrollment",
                "SEND_NOTIFICATION",
                "notification.events",
                Map.of(
                        "type",           "CERTIFICATE_ISSUED",
                        "enrollmentId",   enrollmentId.toString(),
                        "studentId",      enrollment.getStudentId().toString(),
                        "certificateUrl", certificateUrl,
                        "verifyCode",     verifyCode
                )
        );

        log.info("SAGA COMPLETED for enrollment={}", enrollmentId);
    }

    // ── Compensation: Payment failed ─────────────────────────────

    @Transactional
    public void onPaymentFailed(UUID enrollmentId, String reason) {
        Enrollment enrollment = getEnrollment(enrollmentId);
        SagaState saga = getSaga(enrollmentId);
        completeFailed(enrollment, saga, "Payment failed: " + reason);
        log.warn("SAGA FAILED (payment) for enrollment={} reason={}", enrollmentId, reason);
    }

    // ── Compensation: No exam slot available ─────────────────────

    @Transactional
    public void onNoSlotAvailable(UUID enrollmentId) {
        Enrollment enrollment = getEnrollment(enrollmentId);
        SagaState saga = getSaga(enrollmentId);

        // Compensate: refund payment
        outbox.dispatch(
                enrollmentId,
                "Enrollment",
                "REFUND_PAYMENT",
                "payment.inbox.events",
                Map.of("enrollmentId", enrollmentId.toString())
        );

        completeFailed(enrollment, saga, "No exam slots available");
        log.warn("SAGA FAILED (no slot) for enrollment={}", enrollmentId);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void completeFailed(Enrollment enrollment, SagaState saga, String reason) {
        enrollment.setStatus(EnrollmentStatus.FAILED);
        saga.setSagaStatus(SagaStatus.FAILED);
        saga.setFailureReason(reason);
        saga.setCurrentStep(null);

        enrollmentRepository.save(enrollment);
        sagaStateRepository.save(saga);

        outbox.dispatch(
                enrollment.getId(),
                "Enrollment",
                "SEND_NOTIFICATION",
                "notification.events",
                Map.of(
                        "type",         "ENROLLMENT_FAILED",
                        "enrollmentId", enrollment.getId().toString(),
                        "studentId",    enrollment.getStudentId().toString(),
                        "reason",       reason
                )
        );
    }

    private Enrollment getEnrollment(UUID enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalStateException("Enrollment not found: " + enrollmentId));
    }

    private SagaState getSaga(UUID enrollmentId) {
        return sagaStateRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new IllegalStateException("SagaState not found for enrollment: " + enrollmentId));
    }
}
