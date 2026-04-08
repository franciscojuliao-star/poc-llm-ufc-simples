package br.ufc.llm.quiz.service;

import br.ufc.llm.module.domain.Module;
import br.ufc.llm.module.repository.ModuleRepository;
import br.ufc.llm.quiz.domain.Alternative;
import br.ufc.llm.quiz.domain.Question;
import br.ufc.llm.quiz.domain.Quiz;
import br.ufc.llm.quiz.dto.AlternativeRequest;
import br.ufc.llm.quiz.dto.QuestionRequest;
import br.ufc.llm.quiz.dto.QuizRequest;
import br.ufc.llm.quiz.dto.QuizResponse;
import br.ufc.llm.quiz.repository.QuizRepository;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final ModuleRepository moduleRepository;

    public QuizResponse criar(Long moduleId, QuizRequest request) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Módulo não encontrado: " + moduleId));

        if (quizRepository.existsByModuleId(moduleId)) {
            throw new RegraDeNegocioException("Quiz já existe para o módulo: " + moduleId);
        }

        for (int i = 0; i < request.questions().size(); i++) {
            long corretas = request.questions().get(i).alternatives().stream().filter(AlternativeRequest::correct).count();
            if (corretas != 1) {
                throw new RegraDeNegocioException("A pergunta " + (i + 1) + " deve ter exatamente 1 alternativa correta");
            }
        }

        Quiz quiz = Quiz.builder().module(module).build();

        for (int i = 0; i < request.questions().size(); i++) {
            QuestionRequest qr = request.questions().get(i);
            Question question = Question.builder()
                    .statement(qr.statement())
                    .points(qr.points())
                    .orderNum(i + 1)
                    .quiz(quiz)
                    .build();
            List<Alternative> alternatives = qr.alternatives().stream()
                    .map(a -> Alternative.builder().text(a.text()).correct(a.correct()).question(question).build())
                    .toList();
            question.setAlternatives(alternatives);
            quiz.getQuestions().add(question);
        }

        return QuizResponse.from(quizRepository.save(quiz));
    }

    public QuizResponse buscarPorModulo(Long moduleId) {
        return quizRepository.findByModuleId(moduleId)
                .map(QuizResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quiz não encontrado para o módulo: " + moduleId));
    }

}
