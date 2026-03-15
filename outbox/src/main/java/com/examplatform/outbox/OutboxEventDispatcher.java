package com.examplatform.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Writes an OutboxEvent to the local database within the SAME transaction
 * as the domain write. Debezium CDC picks it up from the outbox_events table
 * via PostgreSQL WAL and publishes it to the target Kafka topic.
 *
 * Usage:
 *   outboxEventDispatcher.dispatch(enrollmentId, "Enrollment", "ENROLLMENT_INITIATED",
 *                                  "payment.inbox.events", payloadObject);
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventDispatcher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Saves the event to outbox_events in the current transaction.
     * Must be called inside an active @Transactional context.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void dispatch(UUID aggregateId,
                         String aggregateType,
                         String eventType,
                         String topic,
                         Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventType(eventType)
                    .topic(topic)
                    .payload(payloadJson)
                    .build();

            outboxEventRepository.save(event);
            log.debug("Outbox event saved: type={}, aggregateId={}, topic={}",
                    eventType, aggregateId, topic);

        } catch (JsonProcessingException e) {
            throw new OutboxDispatchException("Failed to serialize outbox payload for event: " + eventType, e);
        }
    }
}
