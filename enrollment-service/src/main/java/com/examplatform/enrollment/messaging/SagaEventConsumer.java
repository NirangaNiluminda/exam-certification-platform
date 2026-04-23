package com.examplatform.enrollment.messaging;

import com.examplatform.enrollment.saga.EnrollmentSaga;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Listens for SAGA reply events from participant services:
 *   - payment-service     → PAYMENT_COMPLETED | PAYMENT_FAILED
 *   - exam-service        → EXAM_SLOT_SCHEDULED | NO_SLOT_AVAILABLE | EXAM_SUBMITTED
 *   - grading-service     → EXAM_GRADED
 *   - certificate-service → CERTIFICATE_ISSUED
 *
 * Routes each event to the appropriate EnrollmentSaga handler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaEventConsumer {

    private final EnrollmentSaga enrollmentSaga;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "enrollment.inbox.events",
                   groupId = "enrollment-saga-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType    = event.get("eventType").asText();
            UUID   enrollmentId = UUID.fromString(event.get("enrollmentId").asText());

            log.info("SAGA event received: type={} enrollmentId={}", eventType, enrollmentId);

            switch (eventType) {
                case "PAYMENT_COMPLETED" ->
                    enrollmentSaga.onPaymentCompleted(
                            enrollmentId,
                            event.get("amount").asDouble());

                case "PAYMENT_FAILED" ->
                    enrollmentSaga.onPaymentFailed(
                            enrollmentId,
                            event.path("reason").asText("Unknown reason"));

                case "EXAM_SLOT_SCHEDULED" ->
                    enrollmentSaga.onExamSlotScheduled(
                            enrollmentId,
                            UUID.fromString(event.get("slotId").asText()),
                            event.get("scheduledAt").asText());

                case "NO_SLOT_AVAILABLE" ->
                    enrollmentSaga.onNoSlotAvailable(enrollmentId);

                case "EXAM_SUBMITTED" ->
                    enrollmentSaga.onExamSubmitted(
                            enrollmentId,
                            UUID.fromString(event.get("slotId").asText()));

                case "EXAM_GRADED" ->
                    enrollmentSaga.onExamGraded(
                            enrollmentId,
                            event.get("passed").asBoolean(),
                            event.path("scorePercentage").asDouble(),
                            event.path("studentName").asText(""),
                            event.path("examTitle").asText(""));

                case "CERTIFICATE_ISSUED" ->
                    enrollmentSaga.onCertificateIssued(
                            enrollmentId,
                            event.get("downloadUrl").asText(),
                            event.get("verifyCode").asText());

                default -> log.warn("Unknown SAGA event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process SAGA event: {}", message, e);
        }
    }
}
