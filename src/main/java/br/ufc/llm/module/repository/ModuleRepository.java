package br.ufc.llm.module.repository;

import br.ufc.llm.module.domain.Module;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ModuleRepository extends R2dbcRepository<Module, Long> {
    Flux<Module> findByCourseIdOrderByOrderNumAsc(Long courseId);
    Mono<Long> countByCourseId(Long courseId);
}
