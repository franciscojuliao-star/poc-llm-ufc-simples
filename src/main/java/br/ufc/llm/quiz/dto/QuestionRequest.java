package br.ufc.llm.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuestionRequest(
        @NotBlank(message = "Enunciado é obrigatório")
        String statement,

        @Min(value = 1, message = "Pontos devem ser no mínimo 1")
        int points,

        @NotEmpty(message = "A pergunta deve ter ao menos uma alternativa")
        @Size(min = 2, message = "A pergunta deve ter no mínimo 2 alternativas")
        @Valid
        List<AlternativeRequest> alternatives
) {}
