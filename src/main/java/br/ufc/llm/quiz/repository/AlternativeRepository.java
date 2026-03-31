package br.ufc.llm.quiz.repository;

import br.ufc.llm.quiz.domain.Alternative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlternativeRepository extends JpaRepository<Alternative, Long> {
    List<Alternative> findByQuestionIdOrderById(Long questionId);
}
