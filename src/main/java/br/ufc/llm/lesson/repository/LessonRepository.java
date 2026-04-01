package br.ufc.llm.lesson.repository;

import br.ufc.llm.lesson.domain.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByModuleIdOrderByOrderNumAsc(Long moduleId);
    int countByModuleId(Long moduleId);
}
