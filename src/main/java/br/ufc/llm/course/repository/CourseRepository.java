package br.ufc.llm.course.repository;

import br.ufc.llm.course.domain.Course;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface CourseRepository extends R2dbcRepository<Course, Long> {
    Flux<Course> findAllBy(Pageable pageable);
}
