package br.ufc.llm.module.dto;

import jakarta.validation.constraints.NotBlank;

public record ModuleRequest(
        @NotBlank String name
) {}
