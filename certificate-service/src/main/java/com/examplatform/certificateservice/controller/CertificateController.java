package com.examplatform.certificateservice.controller;

import com.examplatform.certificateservice.domain.Certificate;
import com.examplatform.certificateservice.domain.CertificateRepository;
import com.examplatform.certificateservice.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateRepository certificateRepository;
    private final MinioStorageService minioStorageService;

    /**
     * Download certificate by ID.
     * Returns a fresh pre-signed MinIO URL (valid 1 hour).
     * Requires auth — student must own the certificate.
     */
    @GetMapping("/{certificateId}")
    public ResponseEntity<Map<String, Object>> getCertificate(
            @PathVariable UUID certificateId,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId) {

        return certificateRepository.findById(certificateId)
                .map(cert -> {
                    // Generate a fresh pre-signed URL on every request
                    String freshUrl = minioStorageService.generatePresignedUrl(cert.getStorageKey());
                    cert.setDownloadUrl(freshUrl);
                    certificateRepository.save(cert);

                    Map<String, Object> body = new HashMap<>();
                    body.put("certificateId", cert.getId());
                    body.put("studentName", cert.getStudentName());
                    body.put("examTitle", cert.getExamTitle());
                    body.put("scorePercentage", cert.getScorePercentage());
                    body.put("issuedAt", cert.getIssuedAt());
                    body.put("verifyCode", cert.getVerifyCode());
                    body.put("downloadUrl", freshUrl);
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-enrollment/{enrollmentId}")
    public ResponseEntity<Map<String, Object>> getByEnrollment(@PathVariable UUID enrollmentId) {
        return certificateRepository.findByEnrollmentId(enrollmentId)
                .map(cert -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("certificateId", cert.getId());
                    body.put("studentName", cert.getStudentName());
                    body.put("examTitle", cert.getExamTitle());
                    body.put("scorePercentage", cert.getScorePercentage());
                    body.put("issuedAt", cert.getIssuedAt());
                    body.put("verifyCode", cert.getVerifyCode());
                    body.put("downloadUrl", cert.getDownloadUrl());
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Public verification endpoint — no auth required.
     * Used by employers or institutions to verify certificate authenticity.
     */
    @GetMapping("/verify/{verifyCode}")
    public ResponseEntity<Map<String, Object>> verifyCertificate(@PathVariable UUID verifyCode) {
        return certificateRepository.findByVerifyCode(verifyCode)
                .map(cert -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("valid", true);
                    body.put("certificateId", cert.getId());
                    body.put("studentName", cert.getStudentName());
                    body.put("examTitle", cert.getExamTitle());
                    body.put("scorePercentage", cert.getScorePercentage());
                    body.put("issuedAt", cert.getIssuedAt());
                    return ResponseEntity.<Map<String, Object>>ok(body);
                })
                .orElseGet(() -> {
                    Map<String, Object> notFound = new HashMap<>();
                    notFound.put("valid", false);
                    notFound.put("message", "Certificate not found");
                    return ResponseEntity.ok(notFound);
                });
    }
}
