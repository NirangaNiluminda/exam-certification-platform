package com.examplatform.gradingservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnswerKeyRepository extends JpaRepository<AnswerKey, UUID> {
    List<AnswerKey> findAllByExamId(UUID examId);
    Optional<AnswerKey> findByQuestionId(UUID questionId);
}
