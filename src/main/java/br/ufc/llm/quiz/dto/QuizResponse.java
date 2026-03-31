package br.ufc.llm.quiz.dto;

import br.ufc.llm.quiz.domain.Quiz;

import java.util.List;

public record QuizResponse(
        Long id,
        Long moduleId,
        boolean showWrongAnswers,
        boolean showCorrectAnswers,
        boolean showPoints,
        List<QuestionResponse> questions
) {
    public static QuizResponse from(Quiz quiz) {
        List<QuestionResponse> questions = quiz.getQuestions().stream()
                .map(QuestionResponse::from)
                .toList();
        return new QuizResponse(
                quiz.getId(),
                quiz.getModule().getId(),
                quiz.isShowWrongAnswers(),
                quiz.isShowCorrectAnswers(),
                quiz.isShowPoints(),
                questions
        );
    }
}
