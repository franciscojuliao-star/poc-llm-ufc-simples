package br.ufc.llm.quiz;

import br.ufc.llm.lesson.domain.Lesson;
import br.ufc.llm.lesson.repository.LessonRepository;
import br.ufc.llm.module.domain.Module;
import br.ufc.llm.module.repository.ModuleRepository;
import br.ufc.llm.quiz.dto.AlternativeResponse;
import br.ufc.llm.quiz.dto.QuestionResponse;
import br.ufc.llm.quiz.dto.QuizGeneratedResponse;
import br.ufc.llm.quiz.dto.QuizResponse;
import br.ufc.llm.quiz.repository.QuizRepository;
import br.ufc.llm.quiz.service.QuizAiService;
import br.ufc.llm.shared.client.RagIntegracaoClient;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizAiServiceTest {

    @Mock private ModuleRepository moduleRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private RagIntegracaoClient ragClient;

    private QuizAiService service;

    @BeforeEach
    void setUp() {
        service = new QuizAiService(moduleRepository, lessonRepository, quizRepository, ragClient);
    }

    private Module moduleMock() {
        return Module.builder().id(1L).name("M1").orderNum(1).build();
    }

    private QuizGeneratedResponse quizMock(String statement) {
        var alternative = new AlternativeResponse(null, "Resposta", true);
        var question = new QuestionResponse(null, statement, 1, 0, List.of(alternative));
        return new QuizGeneratedResponse(List.of(question));
    }

    @Test
    void deveGerarQuizSemSalvarNoBanco() {
        Module module = moduleMock();
        Lesson lesson = Lesson.builder().id(1L).name("Aula 1").orderNum(1)
                .contentEditor("<p>Conteúdo sobre Java</p>").module(module).build();

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(lessonRepository.findByModuleIdOrderByOrderNumAsc(1L)).thenReturn(List.of(lesson));
        when(ragClient.gerarQuiz(anyString(), eq(5))).thenReturn(quizMock("O que é Java?"));

        QuizGeneratedResponse response = service.gerarQuiz(1L, 5);

        assertThat(response.questions()).hasSize(1);
        assertThat(response.questions().get(0).statement()).isEqualTo("O que é Java?");
        verifyNoInteractions(quizRepository);
    }

    @Test
    void deveLancarExcecaoQuandoModuloNaoEncontrado() {
        when(moduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.gerarQuiz(99L, 5))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveLancarExcecaoQuandoNaoHaConteudoLegivel() {
        Module module = moduleMock();
        Lesson lesson = Lesson.builder().id(1L).name("Aula vazia").orderNum(1).module(module).build();

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(lessonRepository.findByModuleIdOrderByOrderNumAsc(1L)).thenReturn(List.of(lesson));

        assertThatThrownBy(() -> service.gerarQuiz(1L, 5))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("conteúdo legível");
    }

    @Test
    void deveLancarExcecaoQuandoRagRetornarErro() {
        Module module = moduleMock();
        Lesson lesson = Lesson.builder().id(1L).name("Aula 1").orderNum(1)
                .contentEditor("Conteúdo").module(module).build();

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(lessonRepository.findByModuleIdOrderByOrderNumAsc(1L)).thenReturn(List.of(lesson));
        when(ragClient.gerarQuiz(anyString(), anyInt()))
                .thenThrow(new RegraDeNegocioException("RAG indisponível"));

        assertThatThrownBy(() -> service.gerarQuiz(1L, 5))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveBuscarQuizPendente() {
        Module module = moduleMock();
        Lesson lesson = Lesson.builder().id(1L).name("Aula 1").orderNum(1)
                .contentEditor("Conteúdo").module(module).build();

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(lessonRepository.findByModuleIdOrderByOrderNumAsc(1L)).thenReturn(List.of(lesson));
        when(ragClient.gerarQuiz(anyString(), eq(5))).thenReturn(quizMock("Pergunta?"));

        service.gerarQuiz(1L, 5);

        assertThat(service.buscarPendente(1L).questions()).hasSize(1);
    }

    @Test
    void deveLancarExcecaoQuandoNaoHaQuizPendente() {
        assertThatThrownBy(() -> service.buscarPendente(1L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Nenhum quiz pendente");
    }

    @Test
    void deveConfirmarQuizEPersistirNoBanco() {
        Module module = moduleMock();
        Lesson lesson = Lesson.builder().id(1L).name("Aula 1").orderNum(1)
                .contentEditor("Conteúdo").module(module).build();

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(lessonRepository.findByModuleIdOrderByOrderNumAsc(1L)).thenReturn(List.of(lesson));
        when(quizRepository.existsByModuleId(1L)).thenReturn(false);
        when(ragClient.gerarQuiz(anyString(), eq(5))).thenReturn(quizMock("Pergunta?"));
        when(quizRepository.save(any())).thenAnswer(inv -> {
            var q = inv.getArgument(0, br.ufc.llm.quiz.domain.Quiz.class);
            q.setId(1L);
            return q;
        });

        service.gerarQuiz(1L, 5);
        QuizResponse response = service.confirmarQuiz(1L);

        assertThat(response.moduleId()).isEqualTo(1L);
        verify(quizRepository).save(any());
        assertThatThrownBy(() -> service.buscarPendente(1L))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveLancarExcecaoAoConfirmarSemPendente() {
        assertThatThrownBy(() -> service.confirmarQuiz(1L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Nenhum quiz pendente");
    }

    @Test
    void deveRegerarQuizSobrescrevendoPendente() {
        Module module = moduleMock();
        Lesson lesson = Lesson.builder().id(1L).name("Aula 1").orderNum(1)
                .contentEditor("Conteúdo").module(module).build();

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(lessonRepository.findByModuleIdOrderByOrderNumAsc(1L)).thenReturn(List.of(lesson));
        when(ragClient.gerarQuiz(anyString(), eq(5)))
                .thenReturn(quizMock("Primeira?"))
                .thenReturn(quizMock("Segunda?"));

        service.gerarQuiz(1L, 5);
        QuizGeneratedResponse novo = service.gerarQuiz(1L, 5);

        assertThat(novo.questions().get(0).statement()).isEqualTo("Segunda?");
        assertThat(service.buscarPendente(1L).questions().get(0).statement()).isEqualTo("Segunda?");
    }
}
