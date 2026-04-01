package br.ufc.llm.module.dto;

import br.ufc.llm.module.domain.Module;

import java.time.LocalDateTime;

public record ModuleResponse(
        Long id,
        String name,
        int orderNum,
        String imagePath,
        Long courseId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ModuleResponse from(Module module) {
        return new ModuleResponse(
                module.getId(),
                module.getName(),
                module.getOrderNum(),
                module.getImagePath(),
                module.getCourse().getId(),
                module.getCreatedAt(),
                module.getUpdatedAt()
        );
    }
}
