package br.ufc.llm.module.repository;

import br.ufc.llm.module.domain.Module;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModuleRepository extends JpaRepository<Module, Long> {
    List<Module> findByCourseIdOrderByOrderNumAsc(Long courseId);
    int countByCourseId(Long courseId);
}
