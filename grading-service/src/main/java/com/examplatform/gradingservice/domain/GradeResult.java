package com.examplatform.gradingservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "grade_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "enrollment_id", nullable = false, unique = true)
    private UUID enrollmentId;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "student_email")
    private String studentEmail;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "score_percentage", nullable = false)
    private Integer scorePercentage;  // 0–100

    @Column(name = "passing_score", nullable = false)
    private Integer passingScore;     // threshold from exam definition

    @Column(nullable = false)
    private Boolean passed;

    @Column(name = "tab_switch_count")
    private Integer tabSwitchCount;

    @CreationTimestamp
    @Column(name = "graded_at", updatable = false)
    private Instant gradedAt;
}
