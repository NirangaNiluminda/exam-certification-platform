package com.examplatform.examservice.domain;

public enum SlotStatus {
    SCHEDULED,   // slot created, exam not yet started
    IN_PROGRESS, // student started the exam
    SUBMITTED,   // student submitted answers
    TIMED_OUT,   // server-side timer expired before submission
    CANCELLED    // enrollment was cancelled
}
