package com.examplatform.examservice.service;

import com.examplatform.examservice.controller.dto.AnswerSubmission;
import com.examplatform.examservice.controller.dto.SubmitExamRequest;
import com.examplatform.examservice.domain.*;
import com.examplatform.outbox.OutboxEventDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamSubmissionService {

    private final ExamSlotRepository examSlotRepository;
    private final AnswerRepository answerRepository;
    private final OutboxEventDispatcher outboxEventDispatcher;

    @Transactional
    public Map<String, Object> submit(UUID slotId, UUID studentId,
                                      int tabSwitchCount, SubmitExamRequest request) {

        ExamSlot slot = examSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        if (!slot.getStudentId().equals(studentId)) {
            throw new SecurityException("Slot does not belong to student");
        }
        if (slot.getStatus() != SlotStatus.IN_PROGRESS) {
            throw new IllegalStateException("Exam is not in progress. Status: " + slot.getStatus());
        }

        // Server-side timer enforcement — reject late submissions
        Instant now = Instant.now();
        if (slot.getDeadlineAt() != null && now.isAfter(slot.getDeadlineAt())) {
            slot.setStatus(SlotStatus.TIMED_OUT);
            examSlotRepository.save(slot);
            throw new IllegalStateException("Exam deadline has passed");
        }

        // Record anti-cheat tab-switch count
        slot.setTabSwitchCount(tabSwitchCount);
        slot.setStatus(SlotStatus.SUBMITTED);
        slot.setSubmittedAt(now);
        examSlotRepository.save(slot);

        // Persist answers
        List<Answer> answers = new ArrayList<>();
        for (AnswerSubmission sub : request.answers()) {
            answers.add(Answer.builder()
                    .slotId(slotId)
                    .questionId(sub.questionId())
                    .selectedOption(sub.selectedOption())
                    .build());
        }
        answerRepository.saveAll(answers);

        // Build payload for grading-service
        List<Map<String, String>> answerPayload = new ArrayList<>();
        for (Answer a : answers) {
            Map<String, String> entry = new HashMap<>();
            entry.put("questionId", a.getQuestionId().toString());
            entry.put("selectedOption", a.getSelectedOption());
            answerPayload.add(entry);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", "EXAM_SUBMITTED");
        payload.put("enrollmentId", slot.getEnrollmentId().toString());
        payload.put("slotId", slotId.toString());
        payload.put("examId", slot.getExam().getId().toString());
        payload.put("studentId", studentId.toString());
        payload.put("studentEmail", slot.getStudentEmail());
        payload.put("studentName", slot.getStudentName());
        payload.put("answers", answerPayload);
        payload.put("tabSwitchCount", tabSwitchCount);
        payload.put("submittedAt", now.toString());

        // Notify enrollment-service that exam was submitted
        outboxEventDispatcher.dispatch(
                slot.getEnrollmentId(),
                "ExamSlot",
                "EXAM_SUBMITTED",
                "enrollment.inbox.events",
                payload
        );

        // Also dispatch to grading inbox
        outboxEventDispatcher.dispatch(
                slotId,
                "ExamSlot",
                "EXAM_SUBMITTED",
                "grading.inbox.events",
                payload
        );

        log.info("Exam submitted: slotId={}, tabSwitches={}", slotId, tabSwitchCount);

        return Map.of(
                "slotId", slotId,
                "status", "SUBMITTED",
                "submittedAt", now.toString(),
                "message", "Exam submitted successfully. Grading in progress."
        );
    }
}
