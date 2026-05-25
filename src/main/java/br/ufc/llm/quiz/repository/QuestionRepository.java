package br.ufc.llm.quiz.repository;

import br.ufc.llm.quiz.domain.Question;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface QuestionRepository extends R2dbcRepository<Question, Long> {
    Flux<Question> findByQuizIdOrderByOrderNumAsc(Long quizId);
    Mono<Long> countByQuizId(Long quizId);
}
