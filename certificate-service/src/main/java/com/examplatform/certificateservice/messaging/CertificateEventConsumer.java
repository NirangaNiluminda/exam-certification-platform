package com.examplatform.certificateservice.messaging;

import com.examplatform.certificateservice.domain.*;
import com.examplatform.certificateservice.service.MinioStorageService;
import com.examplatform.certificateservice.service.PdfCertificateGenerator;
import com.examplatform.outbox.OutboxEventDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes EXAM_GRADED events from certificate.inbox.events.
 * Generates PDF certificate using iText, stores in MinIO, then
 * publishes CERTIFICATE_ISSUED to enrollment.inbox.events and notification.events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateEventConsumer {

    private final CertificateRepository certificateRepository;
    private final EventLogRepository eventLogRepository;
    private final PdfCertificateGenerator pdfGenerator;
    private final MinioStorageService minioStorageService;
    private final OutboxEventDispatcher outboxEventDispatcher;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "certificate.inbox.events",
            groupId = "certificate-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handle(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.path("eventType").asText();
            String eventId = node.path("eventId").asText();

            if (eventLogRepository.existsByEventId(eventId)) {
                log.debug("Duplicate certificate event skipped: {}", eventId);
                return;
            }

            log.info("Received certificate event: type={}, eventId={}", eventType, eventId);

            if ("EXAM_GRADED".equals(eventType)) {
                issueCertificate(node);
            } else {
                log.warn("Unknown certificate event: {}", eventType);
            }

            eventLogRepository.save(EventLog.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .build());

        } catch (Exception e) {
            log.error("Failed to process certificate event: {}", message, e);
            throw new RuntimeException("Certificate event processing failed", e);
        }
    }

    private void issueCertificate(JsonNode node) {
        UUID enrollmentId = UUID.fromString(node.path("enrollmentId").asText());
        UUID studentId = UUID.fromString(node.path("studentId").asText());
        String studentEmail = node.path("studentEmail").asText();
        String studentName = node.path("studentName").asText();
        UUID examId = UUID.fromString(node.path("examId").asText());
        String examTitle = node.path("examTitle").asText(node.path("exam_title").asText("Certification Exam"));
        int scorePercentage = node.path("scorePercentage").asInt();

        // Idempotency — certificate may already be issued
        if (certificateRepository.existsByEnrollmentId(enrollmentId)) {
            log.info("Certificate already issued for enrollment {}, republishing", enrollmentId);
            Certificate existing = certificateRepository.findByEnrollmentId(enrollmentId).orElseThrow();
            publishCertificateIssued(existing);
            return;
        }

        // Create certificate record
        Certificate cert = Certificate.builder()
                .enrollmentId(enrollmentId)
                .studentId(studentId)
                .studentEmail(studentEmail)
                .studentName(studentName)
                .examId(examId)
                .examTitle(examTitle)
                .scorePercentage(scorePercentage)
                .status(CertificateStatus.PENDING)
                .build();
        certificateRepository.save(cert);

        try {
            // Generate PDF
            byte[] pdfBytes = pdfGenerator.generate(cert);

            // Upload to MinIO
            String objectKey = "certificates/" + cert.getId() + ".pdf";
            minioStorageService.upload(objectKey, pdfBytes);

            // Generate pre-signed download URL
            String downloadUrl = minioStorageService.generatePresignedUrl(objectKey);

            // Update certificate with storage info
            cert.setStorageKey(objectKey);
            cert.setDownloadUrl(downloadUrl);
            cert.setStatus(CertificateStatus.ISSUED);
            certificateRepository.save(cert);

            log.info("Certificate issued: id={}, enrollment={}, student={}",
                    cert.getId(), enrollmentId, studentName);

            publishCertificateIssued(cert);

        } catch (Exception e) {
            cert.setStatus(CertificateStatus.FAILED);
            certificateRepository.save(cert);
            log.error("Failed to generate/store certificate for enrollment {}", enrollmentId, e);
            throw e;
        }
    }

    private void publishCertificateIssued(Certificate cert) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", "CERTIFICATE_ISSUED");
        payload.put("enrollmentId", cert.getEnrollmentId().toString());
        payload.put("certificateId", cert.getId().toString());
        payload.put("studentId", cert.getStudentId().toString());
        payload.put("studentEmail", cert.getStudentEmail());
        payload.put("studentName", cert.getStudentName());
        payload.put("examTitle", cert.getExamTitle());
        payload.put("scorePercentage", cert.getScorePercentage());
        payload.put("verifyCode", cert.getVerifyCode().toString());
        payload.put("downloadUrl", cert.getDownloadUrl());

        // Notify enrollment SAGA to complete
        outboxEventDispatcher.dispatch(
                cert.getEnrollmentId(),
                "Certificate",
                "CERTIFICATE_ISSUED",
                "enrollment.inbox.events",
                payload
        );

        // Notify student via notification service
        outboxEventDispatcher.dispatch(
                cert.getId(),
                "Certificate",
                "CERTIFICATE_ISSUED",
                "notification.events",
                payload
        );

        log.info("Published CERTIFICATE_ISSUED for enrollment {}", cert.getEnrollmentId());
    }
}
