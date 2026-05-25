package br.ufc.llm.quiz.repository;

import br.ufc.llm.quiz.domain.Alternative;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface AlternativeRepository extends R2dbcRepository<Alternative, Long> {
    Flux<Alternative> findByQuestionIdOrderById(Long questionId);
}
