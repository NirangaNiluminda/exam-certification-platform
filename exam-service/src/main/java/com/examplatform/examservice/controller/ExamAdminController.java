package com.examplatform.examservice.controller;

import com.examplatform.examservice.controller.dto.ExamRequest;
import com.examplatform.examservice.controller.dto.QuestionAdminResponse;
import com.examplatform.examservice.controller.dto.QuestionRequest;
import com.examplatform.examservice.domain.*;
import com.examplatform.outbox.OutboxEventDispatcher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin endpoints for managing exams and questions.
 * Requires ADMIN or EXAMINER role.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'EXAMINER')")
public class ExamAdminController {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final OutboxEventDispatcher outboxEventDispatcher;

    // ── Exam CRUD ──────────────────────────────────────────────────

    @PostMapping("/exams")
    public ResponseEntity<Exam> createExam(@Valid @RequestBody ExamRequest req) {
        Exam exam = Exam.builder()
                .title(req.title())
                .description(req.description())
                .durationMinutes(req.durationMinutes())
                .passingScore(req.passingScore())
                .totalMarks(req.totalMarks())
                .price(req.price())
                .build();
        Exam saved = examRepository.save(exam);
        log.info("Created exam: id={}, title={}", saved.getId(), saved.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/exams")
    public ResponseEntity<List<Exam>> listAllExams() {
        return ResponseEntity.ok(examRepository.findAll());
    }

    @GetMapping("/exams/{examId}")
    public ResponseEntity<Exam> getExam(@PathVariable UUID examId) {
        return examRepository.findById(examId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/exams/{examId}")
    public ResponseEntity<Exam> updateExam(@PathVariable UUID examId,
                                            @Valid @RequestBody ExamRequest req) {
        return examRepository.findById(examId)
                .map(exam -> {
                    exam.setTitle(req.title());
                    exam.setDescription(req.description());
                    exam.setDurationMinutes(req.durationMinutes());
                    exam.setPassingScore(req.passingScore());
                    exam.setTotalMarks(req.totalMarks());
                    exam.setPrice(req.price());
                    return ResponseEntity.ok(examRepository.save(exam));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/exams/{examId}")
    public ResponseEntity<Map<String, String>> deactivateExam(@PathVariable UUID examId) {
        return examRepository.findById(examId)
                .map(exam -> {
                    exam.setActive(false);
                    examRepository.save(exam);
                    return ResponseEntity.ok(Map.of("message", "Exam deactivated"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Question CRUD ──────────────────────────────────────────────

    @PostMapping("/exams/{examId}/questions")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<QuestionAdminResponse> addQuestion(@PathVariable UUID examId,
                                                              @Valid @RequestBody QuestionRequest req) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + examId));

        Question q = Question.builder()
                .exam(exam)
                .questionText(req.questionText())
                .optionA(req.optionA())
                .optionB(req.optionB())
                .optionC(req.optionC())
                .optionD(req.optionD())
                .correctOption(req.correctOption())
                .marks(req.marks())
                .sortOrder(req.sortOrder())
                .build();
        Question saved = questionRepository.save(q);

        // Dispatch answer key to grading-service via outbox
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", "QUESTION_CREATED");
        payload.put("questionId", saved.getId().toString());
        payload.put("examId", examId.toString());
        payload.put("correctOption", saved.getCorrectOption());
        outboxEventDispatcher.dispatch(examId, "Question", "QUESTION_CREATED",
                "grading.inbox.events", payload);

        log.info("Added question: id={}, examId={}", saved.getId(), examId);
        return ResponseEntity.status(HttpStatus.CREATED).body(QuestionAdminResponse.from(saved));
    }

    @GetMapping("/exams/{examId}/questions")
    public ResponseEntity<List<QuestionAdminResponse>> listQuestions(@PathVariable UUID examId) {
        List<QuestionAdminResponse> questions = questionRepository
                .findAllByExamIdOrderBySortOrderAsc(examId)
                .stream()
                .map(QuestionAdminResponse::from)
                .toList();
        return ResponseEntity.ok(questions);
    }
}
