package br.ufc.llm.lesson.dto;

import br.ufc.llm.lesson.domain.FileType;
import br.ufc.llm.lesson.domain.Lesson;

import java.time.LocalDateTime;

public record LessonResponse(
        Long id,
        String name,
        int orderNum,
        String filePath,
        FileType fileType,
        String contentEditor,
        String contentGenerated,
        Long moduleId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static LessonResponse from(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getName(),
                lesson.getOrderNum(),
                lesson.getFilePath(),
                lesson.getFileType(),
                lesson.getContentEditor(),
                lesson.getContentGenerated(),
                lesson.getModule().getId(),
                lesson.getCreatedAt(),
                lesson.getUpdatedAt()
        );
    }
}
