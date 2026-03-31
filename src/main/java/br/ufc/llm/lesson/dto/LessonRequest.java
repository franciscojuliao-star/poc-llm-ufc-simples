package br.ufc.llm.lesson.dto;

import jakarta.validation.constraints.NotBlank;

public record LessonRequest(
        @NotBlank String name,
        String contentEditor
) {}
