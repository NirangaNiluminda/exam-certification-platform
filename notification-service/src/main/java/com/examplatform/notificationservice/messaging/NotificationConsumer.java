package com.examplatform.notificationservice.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Notification Service — Saga Participant (async, fire-and-forget).
 *
 * Consumes all notification.events and logs them.
 * In production: replace log statements with JavaMailSender or SMS provider.
 *
 * Subscribes to: notification.events
 * No outbox needed — notifications are fire-and-forget.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification.events",
                   groupId = "notification-service-group")
    public void consume(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String type    = event.path("type").asText("UNKNOWN");
            String student = event.path("studentId").asText();

            switch (type) {
                case "EXAM_SCHEDULED" ->
                    log.info("[EMAIL] To student={} → Exam scheduled at: {}",
                            student, event.path("scheduledAt").asText());

                case "CERTIFICATE_ISSUED" ->
                    log.info("[EMAIL] To student={} → Certificate issued! Download: {}",
                            student, event.path("certificateUrl").asText());

                case "ENROLLMENT_FAILED" ->
                    log.info("[EMAIL] To student={} → Enrollment failed. Reason: {}",
                            student, event.path("reason").asText());

                default ->
                    log.info("[NOTIFICATION] type={} student={}", type, student);
            }
        } catch (Exception e) {
            log.error("Failed to process notification event: {}", message, e);
        }
    }
}
