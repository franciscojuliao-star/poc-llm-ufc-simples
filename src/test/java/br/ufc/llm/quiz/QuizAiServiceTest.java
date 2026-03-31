package br.ufc.llm.quiz;

import br.ufc.llm.lesson.domain.Lesson;
import br.ufc.llm.lesson.repository.LessonRepository;
import br.ufc.llm.module.domain.Module;
import br.ufc.llm.module.repository.ModuleRepository;
import br.ufc.llm.quiz.domain.Question;
import br.ufc.llm.quiz.domain.Quiz;
import br.ufc.llm.quiz.dto.QuizGeneratedResponse;
import br.ufc.llm.quiz.dto.QuizResponse;
import br.ufc.llm.quiz.repository.QuizRepository;
import br.ufc.llm.quiz.service.QuizAiService;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizAiServiceTest {

    @Mock private ModuleRepository moduleRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private ChatClient chatClient;

    private QuizAiService service;

    @BeforeEach
    void setUp() {
        service = new QuizAiService(moduleRepository, lessonRepository, quizRepository, chatClient, new ObjectMapper());
    }

    private Module moduleMock() {
        return Module.builder().id(1L).name("M1").orderNum(1).build();
    }

    @SuppressWarnings("unchecked")
    private void mockChatClientJson(String json) {
        var requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(json);
    }

    @Test
    void deveGerarQuizSemSalvarNoBanco() {
        Module module = moduleMock();
        Lesson lesson = Lesson.builder().id(1L).name("Aula 1").orderNum(1)
                .contentEditor("<p>Conteúdo sobre Java</p>").module(module).build();

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(lessonRepository.findByModuleIdOrderByOrderNumAsc(1L)).thenReturn(List.of(lesson));

        mockChatClientJson("""
                [{"statement":"O que é Java?","points":1,"alternatives":[{"text":"Linguagem","correct":true},{"text":"Framework","correct":false}]}]
                """);

        QuizGeneratedResponse response = service.gerarQuiz(1L, 5);

        assertThat(response.questions()).hasSize(1);
        assertThat(response.questions().get(0).statement()).isEqualTo("O que é Java?");
        assertThat(response.questions().get(0).alternatives()).hasSize(2);
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
    void deveLancarExcecaoQuandoIaRetornaJsonInvalido() {
        Module module = moduleMock();
        Lesson lesson = Lesson.builder().id(1L).name("Aula 1").orderNum(1)
                .contentEditor("Conteúdo").module(module).build();

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(lessonRepository.findByModuleIdOrderByOrderNumAsc(1L)).thenReturn(List.of(lesson));

        mockChatClientJson("isso não é JSON válido");

        assertThatThrownBy(() -> service.gerarQuiz(1L, 5))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("IA retornou formato inválido");
    }

    @Test
    void deveBuscarQuizPendente() {
        Module module = moduleMock();
        Lesson lesson = Lesson.builder().id(1L).name("Aula 1").orderNum(1)
                .contentEditor("Conteúdo").module(module).build();

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(lessonRepository.findByModuleIdOrderByOrderNumAsc(1L)).thenReturn(List.of(lesson));

        mockChatClientJson("""
                [{"statement":"Pergunta?","points":1,"alternatives":[{"text":"A","correct":true},{"text":"B","correct":false}]}]
                """);
        service.gerarQuiz(1L, 5);

        QuizGeneratedResponse pendente = service.buscarPendente(1L);

        assertThat(pendente.questions()).hasSize(1);
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
        when(quizRepository.save(any())).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setId(1L);
            return q;
        });

        mockChatClientJson("""
                [{"statement":"Pergunta?","points":1,"alternatives":[{"text":"A","correct":true},{"text":"B","correct":false}]}]
                """);
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

        mockChatClientJson("""
                [{"statement":"Primeira?","points":1,"alternatives":[{"text":"A","correct":true},{"text":"B","correct":false}]}]
                """);
        service.gerarQuiz(1L, 5);

        mockChatClientJson("""
                [{"statement":"Segunda?","points":2,"alternatives":[{"text":"X","correct":true},{"text":"Y","correct":false}]}]
                """);
        QuizGeneratedResponse novo = service.gerarQuiz(1L, 5);

        assertThat(novo.questions().get(0).statement()).isEqualTo("Segunda?");
        assertThat(service.buscarPendente(1L).questions().get(0).statement()).isEqualTo("Segunda?");
    }
}
