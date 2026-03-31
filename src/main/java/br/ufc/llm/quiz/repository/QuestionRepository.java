package br.ufc.llm.quiz.repository;

import br.ufc.llm.quiz.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    int countByQuizId(Long quizId);
    List<Question> findByQuizIdOrderByOrderNumAsc(Long quizId);
}
