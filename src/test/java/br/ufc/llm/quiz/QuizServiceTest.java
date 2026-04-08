package br.ufc.llm.quiz;

import br.ufc.llm.module.domain.Module;
import br.ufc.llm.module.repository.ModuleRepository;
import br.ufc.llm.quiz.domain.Quiz;
import br.ufc.llm.quiz.dto.AlternativeRequest;
import br.ufc.llm.quiz.dto.QuestionRequest;
import br.ufc.llm.quiz.dto.QuizRequest;
import br.ufc.llm.quiz.dto.QuizResponse;
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
}
