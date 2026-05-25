package br.ufc.llm.quiz.service;

import br.ufc.llm.module.repository.ModuleRepository;
import br.ufc.llm.quiz.domain.Alternative;
import br.ufc.llm.quiz.domain.Question;
import br.ufc.llm.quiz.domain.Quiz;
import br.ufc.llm.quiz.dto.*;
import br.ufc.llm.quiz.repository.AlternativeRepository;
import br.ufc.llm.quiz.repository.QuestionRepository;
import br.ufc.llm.quiz.repository.QuizRepository;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final ModuleRepository moduleRepository;
    private final QuestionRepository questionRepository;
    private final AlternativeRepository alternativeRepository;

    public Mono<QuizResponse> criar(Long moduleId, QuizRequest request) {
        for (int i = 0; i < request.questions().size(); i++) {
            long corretas = request.questions().get(i).alternatives().stream()
                    .filter(AlternativeRequest::correct).count();
            if (corretas != 1) {
                return Mono.error(new RegraDeNegocioException(
                        "A pergunta " + (i + 1) + " deve ter exatamente 1 alternativa correta"));
            }
        }

        return moduleRepository.existsById(moduleId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RecursoNaoEncontradoException("Módulo não encontrado: " + moduleId));
                    }
                    return quizRepository.existsByModuleId(moduleId);
                })
                .flatMap(quizExists -> {
                    if (quizExists) {
                        return Mono.error(new RegraDeNegocioException("Quiz já existe para o módulo: " + moduleId));
                    }
                    LocalDateTime now = LocalDateTime.now();
                    Quiz quiz = Quiz.builder().moduleId(moduleId).createdAt(now).updatedAt(now).build();
                    return quizRepository.save(quiz);
                })
                .flatMap(savedQuiz -> salvarQuestoes(savedQuiz, request.questions())
                        .map(questions -> QuizResponse.from(savedQuiz, questions)));
    }

    public Mono<QuizResponse> buscarPorModulo(Long moduleId) {
        return quizRepository.findByModuleId(moduleId)
                .switchIfEmpty(Mono.error(new RecursoNaoEncontradoException(
                        "Quiz não encontrado para o módulo: " + moduleId)))
                .flatMap(quiz -> carregarQuestoes(quiz)
                        .map(questions -> QuizResponse.from(quiz, questions)));
    }

    public Mono<QuizResponse> configurar(Long quizId, QuizConfigRequest request) {
        return quizRepository.findById(quizId)
                .switchIfEmpty(Mono.error(new RecursoNaoEncontradoException("Quiz não encontrado: " + quizId)))
                .flatMap(quiz -> {
                    quiz.setShowWrongAnswers(request.showWrongAnswers());
                    quiz.setShowCorrectAnswers(request.showCorrectAnswers());
                    quiz.setShowPoints(request.showPoints());
                    quiz.setUpdatedAt(LocalDateTime.now());
                    return quizRepository.save(quiz);
                })
                .flatMap(quiz -> carregarQuestoes(quiz).map(qs -> QuizResponse.from(quiz, qs)));
    }

    public Mono<QuestionResponse> adicionarPergunta(Long quizId, QuestionRequest request) {
        long corretas = request.alternatives().stream().filter(AlternativeRequest::correct).count();
        if (corretas != 1) {
            return Mono.error(new RegraDeNegocioException("Cada pergunta deve ter exatamente 1 alternativa correta"));
        }
        return quizRepository.existsById(quizId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RecursoNaoEncontradoException("Quiz não encontrado: " + quizId));
                    }
                    return questionRepository.countByQuizId(quizId);
                })
                .flatMap(count -> {
                    LocalDateTime now = LocalDateTime.now();
                    Question q = Question.builder()
                            .statement(request.statement())
                            .points(request.points())
                            .orderNum(count.intValue() + 1)
                            .quizId(quizId)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return questionRepository.save(q);
                })
                .flatMap(savedQ -> {
                    List<Alternative> alts = request.alternatives().stream()
                            .map(a -> {
                                LocalDateTime now = LocalDateTime.now();
                                return Alternative.builder()
                                        .text(a.text())
                                        .correct(a.correct())
                                        .questionId(savedQ.getId())
                                        .createdAt(now)
                                        .updatedAt(now)
                                        .build();
                            })
                            .toList();
                    return alternativeRepository.saveAll(alts)
                            .map(AlternativeResponse::from)
                            .collectList()
                            .map(altResponses -> QuestionResponse.from(savedQ, altResponses));
                });
    }

    public Mono<List<QuestionResponse>> listarPerguntas(Long quizId) {
        return quizRepository.existsById(quizId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RecursoNaoEncontradoException("Quiz não encontrado: " + quizId));
                    }
                    return questionRepository.findByQuizIdOrderByOrderNumAsc(quizId)
                            .flatMap(q -> alternativeRepository.findByQuestionIdOrderById(q.getId())
                                    .map(AlternativeResponse::from)
                                    .collectList()
                                    .map(alts -> QuestionResponse.from(q, alts)))
                            .collectList();
                });
    }

    public Mono<AlternativeResponse> adicionarAlternativa(Long questionId, AlternativeRequest request) {
        return questionRepository.existsById(questionId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RecursoNaoEncontradoException("Pergunta não encontrada: " + questionId));
                    }
                    LocalDateTime now = LocalDateTime.now();
                    Alternative alt = Alternative.builder()
                            .text(request.text())
                            .correct(request.correct())
                            .questionId(questionId)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return alternativeRepository.save(alt);
                })
                .map(AlternativeResponse::from);
    }

    public Mono<List<AlternativeResponse>> listarAlternativas(Long questionId) {
        return questionRepository.existsById(questionId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RecursoNaoEncontradoException("Pergunta não encontrada: " + questionId));
                    }
                    return alternativeRepository.findByQuestionIdOrderById(questionId)
                            .map(AlternativeResponse::from)
                            .collectList();
                });
    }

    // Usado pelo QuizAiService ao confirmar quiz gerado por IA
    public Mono<QuizResponse> salvarQuizCompleto(Long moduleId, List<QuestionRequest> questoes) {
        return quizRepository.existsByModuleId(moduleId)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RegraDeNegocioException("Quiz já existe para o módulo: " + moduleId));
                    }
                    LocalDateTime now = LocalDateTime.now();
                    return quizRepository.save(Quiz.builder().moduleId(moduleId).createdAt(now).updatedAt(now).build());
                })
                .flatMap(savedQuiz -> salvarQuestoes(savedQuiz, questoes)
                        .map(questions -> QuizResponse.from(savedQuiz, questions)));
    }

    private Mono<List<QuestionResponse>> salvarQuestoes(Quiz quiz, List<QuestionRequest> questoes) {
        return Flux.fromIterable(questoes)
                .index()
                .flatMap(indexedQ -> {
                    QuestionRequest qr = indexedQ.getT2();
                    LocalDateTime now = LocalDateTime.now();
                    Question q = Question.builder()
                            .statement(qr.statement())
                            .points(qr.points())
                            .orderNum(indexedQ.getT1().intValue() + 1)
                            .quizId(quiz.getId())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return questionRepository.save(q)
                            .flatMap(savedQ -> {
                                List<Alternative> alts = qr.alternatives().stream()
                                        .map(a -> {
                                            LocalDateTime n = LocalDateTime.now();
                                            return Alternative.builder()
                                                    .text(a.text())
                                                    .correct(a.correct())
                                                    .questionId(savedQ.getId())
                                                    .createdAt(n)
                                                    .updatedAt(n)
                                                    .build();
                                        })
                                        .toList();
                                return alternativeRepository.saveAll(alts)
                                        .map(AlternativeResponse::from)
                                        .collectList()
                                        .map(altResponses -> QuestionResponse.from(savedQ, altResponses));
                            });
                })
                .collectList();
    }

    private Mono<List<QuestionResponse>> carregarQuestoes(Quiz quiz) {
        return questionRepository.findByQuizIdOrderByOrderNumAsc(quiz.getId())
                .flatMap(q -> alternativeRepository.findByQuestionIdOrderById(q.getId())
                        .map(AlternativeResponse::from)
                        .collectList()
                        .map(alts -> QuestionResponse.from(q, alts)))
                .collectList();
    }
}
