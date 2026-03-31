package br.ufc.llm.quiz.dto;

import java.util.List;

public record QuizGeneratedResponse(
        List<QuestionResponse> questions
) {}
