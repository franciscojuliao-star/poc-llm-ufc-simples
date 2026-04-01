package br.ufc.llm.quiz.repository;

import br.ufc.llm.quiz.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    boolean existsByModuleId(Long moduleId);
    Optional<Quiz> findByModuleId(Long moduleId);
}
