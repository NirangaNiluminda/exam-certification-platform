package com.examplatform.examservice.controller;

import com.examplatform.examservice.domain.*;
import com.examplatform.examservice.service.ExamSubmissionService;
import com.examplatform.examservice.controller.dto.SubmitExamRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Student-facing endpoints for browsing exams, starting slots, and submitting answers.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamRepository examRepository;
    private final ExamSlotRepository examSlotRepository;
    private final QuestionRepository questionRepository;
    private final ExamSubmissionService submissionService;

    /**
     * Browse all active exams (public listing — requires auth via gateway).
     */
    @GetMapping
    public ResponseEntity<List<Exam>> listActiveExams() {
        return ResponseEntity.ok(examRepository.findAllByActiveTrue());
    }

    @GetMapping("/{examId}")
    public ResponseEntity<Exam> getExam(@PathVariable UUID examId) {
        return examRepository.findById(examId)
                .filter(Exam::isActive)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Start the exam for a given slot.
     * Sets status to IN_PROGRESS and records startedAt + deadlineAt.
     * Headers injected by API Gateway: X-Student-Id, X-Student-Email, X-Student-Name.
     */
    @PostMapping("/slots/{slotId}/start")
    public ResponseEntity<Map<String, Object>> startExam(
            @PathVariable UUID slotId,
            @RequestHeader("X-Student-Id") UUID studentId) {

        ExamSlot slot = examSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        if (!slot.getStudentId().equals(studentId)) {
            return ResponseEntity.status(403).build();
        }
        if (slot.getStatus() != SlotStatus.SCHEDULED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Exam already started or finished"));
        }

        Instant now = Instant.now();
        Instant deadline = now.plusSeconds((long) slot.getExam().getDurationMinutes() * 60);
        slot.setStatus(SlotStatus.IN_PROGRESS);
        slot.setStartedAt(now);
        slot.setDeadlineAt(deadline);
        examSlotRepository.save(slot);

        log.info("Slot {} started by student {}. Deadline: {}", slotId, studentId, deadline);

        return ResponseEntity.ok(Map.of(
                "slotId", slotId,
                "startedAt", now.toString(),
                "deadlineAt", deadline.toString(),
                "durationMinutes", slot.getExam().getDurationMinutes()
        ));
    }

    /**
     * Fetch questions for an active exam slot.
     * Questions have no correct answer field — answer keys are in grading-service DB.
     */
    @GetMapping("/slots/{slotId}/questions")
    public ResponseEntity<?> getQuestions(
            @PathVariable UUID slotId,
            @RequestHeader("X-Student-Id") UUID studentId) {

        ExamSlot slot = examSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        if (!slot.getStudentId().equals(studentId)) {
            return ResponseEntity.status(403).build();
        }
        if (slot.getStatus() == SlotStatus.SCHEDULED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Start the exam first"));
        }
        if (slot.getStatus() == SlotStatus.SUBMITTED || slot.getStatus() == SlotStatus.TIMED_OUT) {
            return ResponseEntity.badRequest().body(Map.of("error", "Exam already ended"));
        }
        // Server-side timer enforcement
        if (slot.getDeadlineAt() != null && Instant.now().isAfter(slot.getDeadlineAt())) {
            slot.setStatus(SlotStatus.TIMED_OUT);
            examSlotRepository.save(slot);
            return ResponseEntity.badRequest().body(Map.of("error", "Exam time has expired"));
        }

        List<Question> questions = questionRepository
                .findAllByExamIdOrderBySortOrderAsc(slot.getExam().getId());

        return ResponseEntity.ok(Map.of(
                "slotId", slotId,
                "deadlineAt", slot.getDeadlineAt(),
                "questions", questions
        ));
    }

    /**
     * Submit exam answers.
     * Anti-cheat: X-Tab-Switch-Count header from client is recorded.
     */
    @PostMapping("/slots/{slotId}/submit")
    public ResponseEntity<Map<String, Object>> submitExam(
            @PathVariable UUID slotId,
            @RequestHeader("X-Student-Id") UUID studentId,
            @RequestHeader(value = "X-Tab-Switch-Count", defaultValue = "0") int tabSwitchCount,
            @Valid @RequestBody SubmitExamRequest request) {

        return ResponseEntity.ok(submissionService.submit(slotId, studentId, tabSwitchCount, request));
    }
}
