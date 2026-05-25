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
    public static QuizResponse from(Quiz quiz, List<QuestionResponse> questions) {
        return new QuizResponse(
                quiz.getId(),
                quiz.getModuleId(),
                quiz.isShowWrongAnswers(),
                quiz.isShowCorrectAnswers(),
                quiz.isShowPoints(),
                questions
        );
    }
}
