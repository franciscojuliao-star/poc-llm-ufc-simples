package br.ufc.llm.quiz.service;

import br.ufc.llm.module.domain.Module;
import br.ufc.llm.module.repository.ModuleRepository;
import br.ufc.llm.quiz.domain.Alternative;
import br.ufc.llm.quiz.domain.Question;
import br.ufc.llm.quiz.domain.Quiz;
import br.ufc.llm.quiz.dto.AlternativeRequest;
import br.ufc.llm.quiz.dto.AlternativeResponse;
import br.ufc.llm.quiz.dto.QuestionRequest;
import br.ufc.llm.quiz.dto.QuestionResponse;
import br.ufc.llm.quiz.dto.QuizConfigRequest;
import br.ufc.llm.quiz.dto.QuizRequest;
import br.ufc.llm.quiz.dto.QuizResponse;
import br.ufc.llm.quiz.repository.AlternativeRepository;
import br.ufc.llm.quiz.repository.QuestionRepository;
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
    private final QuestionRepository questionRepository;
    private final AlternativeRepository alternativeRepository;
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

    public QuestionResponse adicionarPergunta(Long quizId, QuestionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quiz não encontrado: " + quizId));

        long corretas = request.alternatives().stream().filter(a -> a.correct()).count();
        if (corretas != 1) {
            throw new RegraDeNegocioException("A pergunta deve ter exatamente 1 alternativa correta");
        }

        int ordem = questionRepository.countByQuizId(quizId) + 1;

        Question question = Question.builder()
                .statement(request.statement())
                .points(request.points())
                .orderNum(ordem)
                .quiz(quiz)
                .build();

        List<Alternative> alternatives = request.alternatives().stream()
                .map(a -> Alternative.builder()
                        .text(a.text())
                        .correct(a.correct())
                        .question(question)
                        .build())
                .toList();
        question.setAlternatives(alternatives);

        return QuestionResponse.from(questionRepository.save(question));
    }

    public List<QuestionResponse> listarPerguntas(Long quizId) {
        quizRepository.findById(quizId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quiz não encontrado: " + quizId));
        return questionRepository.findByQuizIdOrderByOrderNumAsc(quizId).stream()
                .map(QuestionResponse::from)
                .toList();
    }

    public AlternativeResponse adicionarAlternativa(Long questionId, AlternativeRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pergunta não encontrada: " + questionId));
        Alternative alternative = Alternative.builder()
                .text(request.text())
                .correct(request.correct())
                .question(question)
                .build();
        return AlternativeResponse.from(alternativeRepository.save(alternative));
    }

    public QuizResponse configurar(Long quizId, QuizConfigRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quiz não encontrado: " + quizId));
        quiz.setShowWrongAnswers(request.showWrongAnswers());
        quiz.setShowCorrectAnswers(request.showCorrectAnswers());
        quiz.setShowPoints(request.showPoints());
        return QuizResponse.from(quizRepository.save(quiz));
    }

    public List<AlternativeResponse> listarAlternativas(Long questionId) {
        questionRepository.findById(questionId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pergunta não encontrada: " + questionId));
        return alternativeRepository.findByQuestionIdOrderById(questionId).stream()
                .map(AlternativeResponse::from)
                .toList();
    }
}
