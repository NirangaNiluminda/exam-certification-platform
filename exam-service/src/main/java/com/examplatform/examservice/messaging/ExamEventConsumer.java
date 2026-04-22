package com.examplatform.examservice.messaging;

import com.examplatform.examservice.domain.*;
import com.examplatform.outbox.OutboxEventDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes events from exam.inbox.events (published by enrollment-service SAGA).
 * Handles SCHEDULE_EXAM_SLOT — creates an ExamSlot for the enrolled student.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamEventConsumer {

    private final ExamSlotRepository examSlotRepository;
    private final ExamRepository examRepository;
    private final EventLogRepository eventLogRepository;
    private final OutboxEventDispatcher outboxEventDispatcher;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "exam.inbox.events",
            groupId = "exam-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handle(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.path("eventType").asText();
            String eventId = node.path("eventId").asText();

            // Idempotency check
            if (eventLogRepository.existsByEventId(eventId)) {
                log.debug("Duplicate event skipped: {}", eventId);
                return;
            }

            log.info("Received exam event: type={}, eventId={}", eventType, eventId);

            switch (eventType) {
                case "SCHEDULE_EXAM_SLOT" -> handleScheduleSlot(node, eventId);
                default -> log.warn("Unknown exam event type: {}", eventType);
            }

            eventLogRepository.save(EventLog.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .build());

        } catch (Exception e) {
            log.error("Failed to process exam event: {}", message, e);
            throw new RuntimeException("Exam event processing failed", e);
        }
    }

    private void handleScheduleSlot(JsonNode node, String eventId) {
        UUID enrollmentId = UUID.fromString(node.path("enrollmentId").asText());
        UUID examId = UUID.fromString(node.path("examId").asText());
        UUID studentId = UUID.fromString(node.path("studentId").asText());
        String studentEmail = node.path("studentEmail").asText();
        String studentName = node.path("studentName").asText();

        // Idempotency — slot may already exist if we retried
        if (examSlotRepository.existsByEnrollmentId(enrollmentId)) {
            log.info("Exam slot already exists for enrollment {}, skipping", enrollmentId);
            // Still need to re-publish so SAGA can progress
            ExamSlot existing = examSlotRepository.findByEnrollmentId(enrollmentId).orElseThrow();
            publishSlotScheduled(existing);
            return;
        }

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + examId));

        ExamSlot slot = ExamSlot.builder()
                .enrollmentId(enrollmentId)
                .exam(exam)
                .studentId(studentId)
                .studentEmail(studentEmail)
                .studentName(studentName)
                .status(SlotStatus.SCHEDULED)
                .build();

        examSlotRepository.save(slot);
        log.info("Created exam slot {} for enrollment {}", slot.getId(), enrollmentId);

        publishSlotScheduled(slot);
    }

    private void publishSlotScheduled(ExamSlot slot) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", "EXAM_SLOT_SCHEDULED");
        payload.put("enrollmentId", slot.getEnrollmentId().toString());
        payload.put("slotId", slot.getId().toString());
        payload.put("examId", slot.getExam().getId().toString());
        payload.put("examTitle", slot.getExam().getTitle());
        payload.put("durationMinutes", slot.getExam().getDurationMinutes());
        payload.put("studentId", slot.getStudentId().toString());
        payload.put("studentEmail", slot.getStudentEmail());
        payload.put("studentName", slot.getStudentName());
        payload.put("scheduledAt", Instant.now().toString());

        outboxEventDispatcher.dispatch(
                slot.getEnrollmentId(),
                "ExamSlot",
                "EXAM_SLOT_SCHEDULED",
                "enrollment.inbox.events",
                payload
        );
        log.info("Published EXAM_SLOT_SCHEDULED for enrollment {}", slot.getEnrollmentId());
    }
}
