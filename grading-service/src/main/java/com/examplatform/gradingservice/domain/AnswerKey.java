package com.examplatform.gradingservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "answer_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_id", nullable = false, unique = true)
    private UUID questionId;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "correct_option", nullable = false, length = 1)
    private String correctOption; // A, B, C, D
}
