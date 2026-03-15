package com.examplatform.examservice.controller.dto;

import com.examplatform.examservice.domain.Question;

public record QuestionAdminResponse(
        String questionId,
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        Integer marks,
        Integer sortOrder,
        String correctOption
) {
    public static QuestionAdminResponse from(Question q) {
        return new QuestionAdminResponse(
                q.getId().toString(),
                q.getQuestionText(),
                q.getOptionA(),
                q.getOptionB(),
                q.getOptionC(),
                q.getOptionD(),
                q.getMarks(),
                q.getSortOrder(),
                q.getCorrectOption()
        );
    }
}
