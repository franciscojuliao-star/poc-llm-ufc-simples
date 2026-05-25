package br.ufc.llm.lesson.repository;

import br.ufc.llm.lesson.domain.Lesson;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LessonRepository extends R2dbcRepository<Lesson, Long> {
    Flux<Lesson> findByModuleIdOrderByOrderNumAsc(Long moduleId);
    Mono<Long> countByModuleId(Long moduleId);
}
