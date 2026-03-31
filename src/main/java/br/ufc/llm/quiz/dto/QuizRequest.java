package br.ufc.llm.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record QuizRequest(
        @NotEmpty(message = "O quiz deve ter ao menos uma pergunta")
        @Valid
        List<QuestionRequest> questions
) {}
