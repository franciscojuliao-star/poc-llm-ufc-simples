package br.ufc.llm.course.dto;

import jakarta.validation.constraints.NotBlank;

public record CourseRequest(
        @NotBlank String title,
        @NotBlank String category,
        @NotBlank String description
) {}
