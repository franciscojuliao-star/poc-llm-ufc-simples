package br.ufc.llm.course.dto;

import br.ufc.llm.course.domain.Course;

import java.time.LocalDateTime;

public record CourseResponse(
        Long id,
        String title,
        String category,
        String description,
        String imagePath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getCategory(),
                course.getDescription(),
                course.getImagePath(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }
}
