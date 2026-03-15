package com.examplatform.certificateservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "enrollment_id", nullable = false, unique = true)
    private UUID enrollmentId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "student_email", nullable = false)
    private String studentEmail;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "exam_title", nullable = false)
    private String examTitle;

    @Column(name = "score_percentage", nullable = false)
    private Integer scorePercentage;

    /** UUID-based public verification code — used in verify URL */
    @Column(name = "verify_code", nullable = false, unique = true)
    @Builder.Default
    private UUID verifyCode = UUID.randomUUID();

    /** MinIO object key where the PDF is stored */
    @Column(name = "storage_key")
    private String storageKey;

    /** Pre-signed download URL (short-lived; regenerated on demand) */
    @Column(name = "download_url", columnDefinition = "TEXT")
    private String downloadUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CertificateStatus status = CertificateStatus.PENDING;

    @CreationTimestamp
    @Column(name = "issued_at", updatable = false)
    private Instant issuedAt;
}
