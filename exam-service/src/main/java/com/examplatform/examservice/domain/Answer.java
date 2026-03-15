package com.examplatform.examservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "selected_option", nullable = false, length = 1)
    private String selectedOption; // A, B, C, D
}
