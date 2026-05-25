package br.ufc.llm.quiz.repository;

import br.ufc.llm.quiz.domain.Quiz;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface QuizRepository extends R2dbcRepository<Quiz, Long> {
    Mono<Boolean> existsByModuleId(Long moduleId);
    Mono<Quiz> findByModuleId(Long moduleId);
}
