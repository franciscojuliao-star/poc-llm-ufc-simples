package br.ufc.llm.quiz.dto;

public record QuizConfigRequest(
        boolean showWrongAnswers,
        boolean showCorrectAnswers,
        boolean showPoints
) {}
