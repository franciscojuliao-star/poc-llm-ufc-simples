package br.ufc.llm.quiz.dto;

import jakarta.validation.constraints.NotBlank;

public record AlternativeRequest(
        @NotBlank(message = "Texto da alternativa é obrigatório")
        String text,
        boolean correct
) {}
