package com.examplatform.gradingservice.messaging;

import com.examplatform.gradingservice.domain.*;
import com.examplatform.outbox.OutboxEventDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Auto-grades MCQ exams when EXAM_SUBMITTED events arrive.
 * Compares student answers against answer_keys table.
 * Publishes EXAM_GRADED to both enrollment.inbox.events and certificate.inbox.events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GradingEventConsumer {

    private final AnswerKeyRepository answerKeyRepository;
    private final GradeResultRepository gradeResultRepository;
    private final EventLogRepository eventLogRepository;
    private final OutboxEventDispatcher outboxEventDispatcher;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "grading.inbox.events",
            groupId = "grading-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handle(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.path("eventType").asText();
            String eventId = node.path("eventId").asText();

            if (eventLogRepository.existsByEventId(eventId)) {
                log.debug("Duplicate grading event skipped: {}", eventId);
                return;
            }

            log.info("Received grading event: type={}, eventId={}", eventType, eventId);

            if ("EXAM_SUBMITTED".equals(eventType)) {
                grade(node);
            } else if ("QUESTION_CREATED".equals(eventType)) {
                saveAnswerKey(node);
            } else {
                log.warn("Unknown grading event: {}", eventType);
            }

            eventLogRepository.save(EventLog.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .build());

        } catch (Exception e) {
            log.error("Failed to process grading event: {}", message, e);
            throw new RuntimeException("Grading event processing failed", e);
        }
    }

    private void saveAnswerKey(JsonNode node) {
        UUID questionId = UUID.fromString(node.path("questionId").asText());
        UUID examId = UUID.fromString(node.path("examId").asText());
        String correctOption = node.path("correctOption").asText();
        if (answerKeyRepository.findByQuestionId(questionId).isEmpty()) {
            answerKeyRepository.save(AnswerKey.builder()
                    .questionId(questionId)
                    .examId(examId)
                    .correctOption(correctOption)
                    .build());
            log.info("Answer key saved: questionId={}, correctOption={}", questionId, correctOption);
        } else {
            log.debug("Answer key already exists for questionId={}, skipping", questionId);
        }
    }

    private void grade(JsonNode node) {
        UUID enrollmentId = UUID.fromString(node.path("enrollmentId").asText());
        UUID slotId = UUID.fromString(node.path("slotId").asText());
        UUID examId = UUID.fromString(node.path("examId").asText());
        UUID studentId = UUID.fromString(node.path("studentId").asText());
        String studentEmail = node.path("studentEmail").asText();
        String studentName = node.path("studentName").asText();
        int tabSwitchCount = node.path("tabSwitchCount").asInt(0);

        // Idempotency at service level too
        if (gradeResultRepository.existsByEnrollmentId(enrollmentId)) {
            log.info("Grade result already exists for enrollment {}, republishing", enrollmentId);
            GradeResult existing = gradeResultRepository.findByEnrollmentId(enrollmentId).orElseThrow();
            publishGradeResult(existing);
            return;
        }

        // Load answer keys for this exam
        Map<UUID, String> keyMap = new HashMap<>();
        for (AnswerKey ak : answerKeyRepository.findAllByExamId(examId)) {
            keyMap.put(ak.getQuestionId(), ak.getCorrectOption());
        }

        // Grade student answers
        JsonNode answersNode = node.path("answers");
        int correct = 0;
        int total = answersNode.size();

        for (JsonNode ans : answersNode) {
            UUID questionId = UUID.fromString(ans.path("questionId").asText());
            String selected = ans.path("selectedOption").asText();
            String correctOption = keyMap.get(questionId);
            if (correctOption != null && correctOption.equals(selected)) {
                correct++;
            }
        }

        int scorePercentage = total > 0 ? (correct * 100 / total) : 0;
        // Passing threshold is 70% (standard for all exams — could be per-exam in a real system)
        int passingScore = 70;
        boolean passed = scorePercentage >= passingScore;

        GradeResult result = GradeResult.builder()
                .enrollmentId(enrollmentId)
                .slotId(slotId)
                .examId(examId)
                .studentId(studentId)
                .studentEmail(studentEmail)
                .studentName(studentName)
                .correctCount(correct)
                .totalQuestions(total)
                .scorePercentage(scorePercentage)
                .passingScore(passingScore)
                .passed(passed)
                .tabSwitchCount(tabSwitchCount)
                .build();

        gradeResultRepository.save(result);
        log.info("Graded: enrollment={}, score={}%, passed={}, tabSwitches={}",
                enrollmentId, scorePercentage, passed, tabSwitchCount);

        publishGradeResult(result);
    }

    private void publishGradeResult(GradeResult result) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", "EXAM_GRADED");
        payload.put("enrollmentId", result.getEnrollmentId().toString());
        payload.put("slotId", result.getSlotId().toString());
        payload.put("examId", result.getExamId().toString());
        payload.put("studentId", result.getStudentId().toString());
        payload.put("studentEmail", result.getStudentEmail());
        payload.put("studentName", result.getStudentName());
        payload.put("scorePercentage", result.getScorePercentage());
        payload.put("correctCount", result.getCorrectCount());
        payload.put("totalQuestions", result.getTotalQuestions());
        payload.put("passed", result.getPassed());
        payload.put("tabSwitchCount", result.getTabSwitchCount());

        // Notify enrollment SAGA
        outboxEventDispatcher.dispatch(
                result.getEnrollmentId(),
                "GradeResult",
                "EXAM_GRADED",
                "enrollment.inbox.events",
                payload
        );

        // If passed, trigger certificate generation
        if (Boolean.TRUE.equals(result.getPassed())) {
            outboxEventDispatcher.dispatch(
                    result.getEnrollmentId(),
                    "GradeResult",
                    "EXAM_GRADED",
                    "certificate.inbox.events",
                    payload
            );
            log.info("Dispatched certificate request for enrollment {}", result.getEnrollmentId());
        }
    }
}
