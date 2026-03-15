package com.examplatform.paymentservice.messaging;

import com.examplatform.outbox.OutboxEventDispatcher;
import com.examplatform.paymentservice.domain.Payment;
import com.examplatform.paymentservice.domain.PaymentRepository;
import com.examplatform.paymentservice.domain.PaymentStatus;
import com.examplatform.paymentservice.domain.EventLog;
import com.examplatform.paymentservice.domain.EventLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Saga Participant — Payment Service
 * Consumes ENROLLMENT_INITIATED from payment.inbox.events.
 * Processes mock payment, publishes result back to enrollment.inbox.events.
 *
 * Uses EventLog for idempotency — prevents double-charging on Kafka retry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentRepository paymentRepository;
    private final EventLogRepository eventLogRepository;
    private final OutboxEventDispatcher outbox;
    private final ObjectMapper objectMapper;

    @Transactional
    @KafkaListener(topics = "payment.inbox.events",
                   groupId = "payment-service-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(String message) {
        try {
            JsonNode event      = objectMapper.readTree(message);
            String eventType    = event.get("eventType").asText();
            UUID   eventId      = UUID.fromString(event.get("enrollmentId").asText());
            String eventIdStr   = eventId.toString();

            // Idempotency check — skip if already processed
            if (eventLogRepository.existsByEventId(eventIdStr)) {
                log.info("Duplicate payment event skipped: {}", eventIdStr);
                return;
            }

            if ("ENROLLMENT_INITIATED".equals(eventType)) {
                processPayment(event, eventId);
            } else if ("REFUND_PAYMENT".equals(eventType)) {
                processRefund(event);
            }

            eventLogRepository.save(EventLog.builder()
                    .eventId(eventIdStr)
                    .eventType(eventType)
                    .build());

        } catch (Exception e) {
            log.error("Failed to process payment event: {}", message, e);
        }
    }

    private void processPayment(JsonNode event, UUID enrollmentId) {
        UUID   studentId = UUID.fromString(event.get("studentId").asText());
        double amount    = event.get("amount").asDouble();

        // Mock payment — always succeeds unless amount > 1000 (for demo failure path)
        boolean success = amount <= 1000.0;

        Payment payment = Payment.builder()
                .enrollmentId(enrollmentId)
                .studentId(studentId)
                .amount(BigDecimal.valueOf(amount))
                .status(success ? PaymentStatus.COMPLETED : PaymentStatus.FAILED)
                .processedAt(Instant.now())
                .build();
        paymentRepository.save(payment);

        if (success) {
            outbox.dispatch(enrollmentId, "Payment", "PAYMENT_COMPLETED",
                    "enrollment.inbox.events",
                    Map.of("enrollmentId", enrollmentId.toString(),
                           "eventType",    "PAYMENT_COMPLETED",
                           "amount",       amount));
            log.info("Payment COMPLETED for enrollment={} amount={}", enrollmentId, amount);
        } else {
            outbox.dispatch(enrollmentId, "Payment", "PAYMENT_FAILED",
                    "enrollment.inbox.events",
                    Map.of("enrollmentId", enrollmentId.toString(),
                           "eventType",    "PAYMENT_FAILED",
                           "reason",       "Amount exceeds limit"));
            log.warn("Payment FAILED for enrollment={}", enrollmentId);
        }
    }

    private void processRefund(JsonNode event) {
        UUID enrollmentId = UUID.fromString(event.get("enrollmentId").asText());
        paymentRepository.findByEnrollmentId(enrollmentId).ifPresent(p -> {
            p.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(p);
            log.info("Payment REFUNDED for enrollment={}", enrollmentId);
        });
    }
}
