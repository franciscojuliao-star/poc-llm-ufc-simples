package br.ufc.llm.quiz;

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
import br.ufc.llm.quiz.repository.AlternativeRepository;
import br.ufc.llm.quiz.repository.QuestionRepository;
import br.ufc.llm.quiz.repository.QuizRepository;
import br.ufc.llm.quiz.service.QuizService;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock private QuizRepository quizRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private AlternativeRepository alternativeRepository;
    @Mock private ModuleRepository moduleRepository;

    @InjectMocks
    private QuizService service;

    private Module moduleMock() {
        return Module.builder().id(1L).name("M1").orderNum(1).build();
    }

    private Quiz quizMock(Module module) {
        return Quiz.builder().id(1L).module(module).build();
    }

    private QuizRequest requestComUmaPergunta() {
        return new QuizRequest(List.of(
                new QuestionRequest("O que é Java?", 1, List.of(
                        new AlternativeRequest("Linguagem de programação", true),
                        new AlternativeRequest("Framework", false)
                ))
        ));
    }

    @Test
    void deveCriarQuizComPerguntasEAlternativas() {
        Module module = moduleMock();
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(quizRepository.existsByModuleId(1L)).thenReturn(false);
        when(quizRepository.save(any())).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setId(1L);
            return q;
        });

        var response = service.criar(1L, requestComUmaPergunta());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.moduleId()).isEqualTo(1L);
        assertThat(response.questions()).hasSize(1);
        assertThat(response.questions().get(0).alternatives()).hasSize(2);
    }

    @Test
    void deveLancarExcecaoQuandoQuizJaExiste() {
        Module module = moduleMock();
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(quizRepository.existsByModuleId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.criar(1L, requestComUmaPergunta()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já existe");
    }

    @Test
    void deveLancarExcecaoQuandoModuloNaoEncontradoAoCriarQuiz() {
        when(moduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(99L, requestComUmaPergunta()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveLancarExcecaoAoCriarQuizComPerguntaSemAlternativaCorreta() {
        Module module = moduleMock();
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(quizRepository.existsByModuleId(1L)).thenReturn(false);

        var request = new QuizRequest(List.of(
                new QuestionRequest("Pergunta?", 1, List.of(
                        new AlternativeRequest("A", false),
                        new AlternativeRequest("B", false)
                ))
        ));

        assertThatThrownBy(() -> service.criar(1L, request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("exatamente 1 alternativa correta");
    }

    @Test
    void deveLancarExcecaoAoCriarQuizComMaisDeUmaAlternativaCorreta() {
        Module module = moduleMock();
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(quizRepository.existsByModuleId(1L)).thenReturn(false);

        var request = new QuizRequest(List.of(
                new QuestionRequest("Pergunta?", 1, List.of(
                        new AlternativeRequest("A", true),
                        new AlternativeRequest("B", true)
                ))
        ));

        assertThatThrownBy(() -> service.criar(1L, request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("exatamente 1 alternativa correta");
    }

    @Test
    void deveBuscarQuizPorModulo() {
        Module module = moduleMock();
        Quiz quiz = quizMock(module);
        when(quizRepository.findByModuleId(1L)).thenReturn(Optional.of(quiz));

        var response = service.buscarPorModulo(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.moduleId()).isEqualTo(1L);
    }

    @Test
    void deveLancarExcecaoQuandoQuizNaoEncontrado() {
        when(quizRepository.findByModuleId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorModulo(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveAdicionarPerguntaComAlternativas() {
        Module module = moduleMock();
        Quiz quiz = quizMock(module);
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(questionRepository.countByQuizId(1L)).thenReturn(0);
        when(questionRepository.save(any())).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(1L);
            return q;
        });

        var request = new QuestionRequest("O que é Java?", 1, List.of(
                new AlternativeRequest("Linguagem de programação", true),
                new AlternativeRequest("Framework", false)
        ));

        var response = service.adicionarPergunta(1L, request);

        assertThat(response.statement()).isEqualTo("O que é Java?");
        assertThat(response.orderNum()).isEqualTo(1);
        assertThat(response.alternatives()).hasSize(2);
    }

    @Test
    void deveIncrementarOrdemDaPergunta() {
        Module module = moduleMock();
        Quiz quiz = quizMock(module);
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(questionRepository.countByQuizId(1L)).thenReturn(2);
        when(questionRepository.save(any())).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(3L);
            return q;
        });

        var request = new QuestionRequest("Nova pergunta?", 1, List.of(
                new AlternativeRequest("A", true),
                new AlternativeRequest("B", false)
        ));

        var response = service.adicionarPergunta(1L, request);

        assertThat(response.orderNum()).isEqualTo(3);
    }

    @Test
    void deveLancarExcecaoQuandoNaoHaExatamenteUmaAlternativaCorreta() {
        Module module = moduleMock();
        Quiz quiz = quizMock(module);
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));

        var request = new QuestionRequest("Pergunta?", 1, List.of(
                new AlternativeRequest("A", true),
                new AlternativeRequest("B", true)
        ));

        assertThatThrownBy(() -> service.adicionarPergunta(1L, request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("exatamente 1 alternativa correta");
    }

    @Test
    void deveLancarExcecaoQuandoNenhumaAlternativaCorreta() {
        Module module = moduleMock();
        Quiz quiz = quizMock(module);
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));

        var request = new QuestionRequest("Pergunta?", 1, List.of(
                new AlternativeRequest("A", false),
                new AlternativeRequest("B", false)
        ));

        assertThatThrownBy(() -> service.adicionarPergunta(1L, request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("exatamente 1 alternativa correta");
    }

    @Test
    void deveListarPerguntasDoQuiz() {
        Module module = moduleMock();
        Quiz quiz = quizMock(module);
        Question q = Question.builder().id(1L).statement("Q?").points(1).orderNum(1).quiz(quiz).build();
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(questionRepository.findByQuizIdOrderByOrderNumAsc(1L)).thenReturn(List.of(q));

        List<QuestionResponse> lista = service.listarPerguntas(1L);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).statement()).isEqualTo("Q?");
    }

    @Test
    void deveLancarExcecaoAoListarPerguntasDeQuizInexistente() {
        when(quizRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarPerguntas(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveAdicionarAlternativaAPergunta() {
        Question question = Question.builder().id(1L).statement("Q?").points(1).orderNum(1).build();
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(alternativeRepository.save(any())).thenAnswer(inv -> {
            Alternative a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        var response = service.adicionarAlternativa(1L, new AlternativeRequest("Opção A", true));

        assertThat(response.text()).isEqualTo("Opção A");
        assertThat(response.correct()).isTrue();
    }

    @Test
    void deveLancarExcecaoAoAdicionarAlternativaEmPerguntaInexistente() {
        when(questionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adicionarAlternativa(99L, new AlternativeRequest("X", false)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveListarAlternativasDaPergunta() {
        Question question = Question.builder().id(1L).statement("Q?").points(1).orderNum(1).build();
        Alternative alt = Alternative.builder().id(1L).text("A").correct(true).question(question).build();
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(alternativeRepository.findByQuestionIdOrderById(1L)).thenReturn(List.of(alt));

        List<AlternativeResponse> lista = service.listarAlternativas(1L);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).text()).isEqualTo("A");
    }

    @Test
    void deveLancarExcecaoAoListarAlternativasDePerguntaInexistente() {
        when(questionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarAlternativas(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveConfigurarQuiz() {
        Module module = moduleMock();
        Quiz quiz = quizMock(module);
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(any())).thenReturn(quiz);

        var request = new QuizConfigRequest(true, false, true);
        var response = service.configurar(1L, request);

        assertThat(response.showWrongAnswers()).isTrue();
        assertThat(response.showCorrectAnswers()).isFalse();
        assertThat(response.showPoints()).isTrue();
        verify(quizRepository).save(quiz);
    }

    @Test
    void deveLancarExcecaoAoConfigurarQuizInexistente() {
        when(quizRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.configurar(99L, new QuizConfigRequest(true, true, true)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
